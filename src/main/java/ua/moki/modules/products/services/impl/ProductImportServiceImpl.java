package ua.moki.modules.products.services.impl;

import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ua.moki.infrastructure.storage.dtos.TranslatorDTO;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;
import ua.moki.infrastructure.storage.service.TranslatorService;
import ua.moki.modules.products.dtos.ProductImportErrorDTO;
import ua.moki.modules.products.dtos.ProductImportResultDTO;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductWeightOptionDTO;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.enums.ProductType;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.ProductImportService;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.products.utils.ExcelWorkbookHelper;
import ua.moki.modules.products.utils.ExcelWorkbookHelper.ExcelRow;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImportServiceImpl implements ProductImportService {

    private static final Pattern VALID_UNIT_PATTERN = Pattern.compile("(г|кг|мл|л|см|м|шт|-)");

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final GeminiProductGenerator geminiProductGenerator;
    private final TranslatorService translatorService;

    private final Slugify slugify = Slugify.builder().transliterator(true)
            .customReplacement("х", "kh")
            .customReplacement("щ", "shch")
            .customReplacement("ц", "ts")
            .build();

    @Override
    public byte[] generateTemplate() throws IOException {
        return ExcelWorkbookHelper.generateTemplateXlsx();
    }

    @Override
    public ProductImportResultDTO importProductsFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл для імпорту порожній або не переданий");
        }

        List<ExcelRow> rawRows;
        try {
            rawRows = ExcelWorkbookHelper.readFirstSheet(file.getInputStream());
        } catch (IOException e) {
            log.error("Помилка читання Excel файлу: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Не вдалося прочитати Excel файл. Переконайтеся, що це файл формату .xlsx");
        }

        List<ExcelRow> nonEmptyRows = rawRows.stream()
                .filter(r -> !r.isEmpty())
                .toList();

        if (nonEmptyRows.isEmpty()) {
            return new ProductImportResultDTO(0, 0, 0, List.of());
        }

        ExcelRow firstRow = nonEmptyRows.get(0);
        boolean hasHeader = isHeaderRow(firstRow);
        ColumnMap columnMap = hasHeader ? ColumnMap.fromHeaderRow(firstRow) : ColumnMap.defaultPositional(firstRow.cells().size());

        List<ExcelRow> dataRows = hasHeader ? nonEmptyRows.subList(1, nonEmptyRows.size()) : nonEmptyRows;

        int totalRows = dataRows.size();
        int importedCount = 0;
        int skippedCount = 0;
        List<ProductImportErrorDTO> errors = new ArrayList<>();

        for (ExcelRow row : dataRows) {
            String rawName = row.getCell(columnMap.nameIdx);
            String displayProductName = rawName.isBlank() ? "(Без назви)" : rawName;

            try {
                ProductRequestDTO requestDTO = buildProductRequestFromRow(row, columnMap);
                productService.createProduct(requestDTO);
                importedCount++;
            } catch (RowValidationException e) {
                skippedCount++;
                errors.add(new ProductImportErrorDTO(row.rowNumber(), displayProductName, e.getMessage()));
            } catch (Exception e) {
                log.error("Помилка при імпорті рядка {}: {}", row.rowNumber(), e.getMessage(), e);
                skippedCount++;
                errors.add(new ProductImportErrorDTO(row.rowNumber(), displayProductName,
                        e.getMessage() != null ? e.getMessage() : "Невідома помилка збереження товару"));
            }
        }

        return new ProductImportResultDTO(totalRows, importedCount, skippedCount, errors);
    }

    private ProductRequestDTO buildProductRequestFromRow(ExcelRow row, ColumnMap col) {
        String name = row.getCell(col.nameIdx);
        if (name.isBlank()) {
            throw new RowValidationException("Не вказано назву товару");
        }
        if (name.length() < 2 || name.length() > 255) {
            throw new RowValidationException("Назва товару має містити від 2 до 255 символів");
        }

        String slug = slugify.slugify(name.toLowerCase());
        if (slug.isBlank()) {
            throw new RowValidationException("Не вдалося згенерувати slug для назви: " + name);
        }
        if (productRepository.existsBySlug(slug)) {
            throw new RowValidationException("Товар із такою назвою (slug: '" + slug + "') вже існує");
        }

        BigDecimal price = parsePrice(row.getCell(col.priceIdx), "ціна", true);
        BigDecimal purchasePrice = parsePrice(row.getCell(col.purchasePriceIdx), "закупівельна ціна", false);
        Integer discount = parseDiscount(row.getCell(col.discountIdx));

        String manufacturer = row.getCell(col.manufacturerIdx);
        if (manufacturer.isBlank()) {
            throw new RowValidationException("Не вказано виробника товару");
        }
        if (manufacturer.length() < 2 || manufacturer.length() > 64) {
            throw new RowValidationException("Назва виробника має містити від 2 до 64 символів");
        }

        ProductCategory category = parseCategory(row.getCell(col.categoryIdx));

        String subcategory = row.getCell(col.subcategoryIdx);
        if (subcategory.isBlank()) {
            throw new RowValidationException("Не вказано підкатегорію товару");
        }

        ProductType productType = parseProductType(row.getCell(col.productTypeIdx));

        Integer valueOfInitOfMeasure = parseMeasureValue(row.getCell(col.valueOfMeasureIdx));
        String initOfMeasure = parseMeasureUnit(row.getCell(col.unitOfMeasureIdx));

        String rawCharacteristics = row.getCell(col.characteristicsIdx);
        Map<String, String> characteristics = parseCharacteristics(rawCharacteristics);
        String attributesForPrompt = buildAttributesString(characteristics, rawCharacteristics);

        boolean allowCustomWeight = false;
        Integer minCustomWeight = null;
        List<ProductWeightOptionDTO> weightOptions = null;

        if (productType == ProductType.WEIGHT_BASED) {
            allowCustomWeight = parseBoolean(row.getCell(col.allowCustomWeightIdx));
            if (allowCustomWeight) {
                minCustomWeight = parseOptionalPositiveInt(row.getCell(col.minCustomWeightIdx), "Мін. кастомна вага");
                if (minCustomWeight == null) {
                    minCustomWeight = 100;
                }
            }
            weightOptions = parseWeightOptions(row.getCell(col.weightOptionsIdx), price);
        }

        String groupId = null;
        String variantName = null;
        String variantValue = null;

        if (productType == ProductType.VARIANT) {
            groupId = emptyToNull(row.getCell(col.groupIdIdx));
            variantName = emptyToNull(row.getCell(col.variantNameIdx));
            variantValue = emptyToNull(row.getCell(col.variantValueIdx));
            if (groupId != null && groupId.length() > 255) {
                throw new RowValidationException("Group ID не може перевищувати 255 символів");
            }
            if (variantName != null && variantName.length() > 100) {
                throw new RowValidationException("Назва параметра варіації не може перевищувати 100 символів");
            }
            if (variantValue != null && variantValue.length() > 100) {
                throw new RowValidationException("Значення варіації не може перевищувати 100 символів");
            }
        }

        // 1. Генерація опису через власний сервіс GeminiProductGenerator
        String description = geminiProductGenerator.generateDescription(name, attributesForPrompt);
        if (description == null || description.trim().length() < 2) {
            description = attributesForPrompt.isBlank()
                    ? name
                    : name + ". Характеристики: " + attributesForPrompt;
        }
        description = truncate(description.trim(), 5000);

        // 2. Переклад через TranslatorServiceImpl
        TranslatorDTO toTranslate = new TranslatorDTO(name, subcategory, description, characteristics);
        TranslatorDTO translated = translatorService.translateTextsToRu(toTranslate);

        String nameRu = (translated != null && translated.name() != null && translated.name().trim().length() >= 2)
                ? truncate(translated.name().trim(), 255)
                : truncate(name, 255);

        String subcategoryRu = (translated != null && translated.subcategory() != null && !translated.subcategory().isBlank())
                ? translated.subcategory().trim()
                : subcategory;

        String descriptionRu = (translated != null && translated.description() != null && translated.description().trim().length() >= 2)
                ? truncate(translated.description().trim(), 5000)
                : description;

        Map<String, String> characteristicsRu = (translated != null && translated.characteristics() != null)
                ? translated.characteristics()
                : new HashMap<>(characteristics);

        return new ProductRequestDTO(
                name,
                nameRu,
                category,
                description,
                descriptionRu,
                price,
                ProductAvailability.OUT_OF_STOCK,
                discount,
                purchasePrice,
                manufacturer,
                subcategory,
                subcategoryRu,
                initOfMeasure,
                valueOfInitOfMeasure,
                List.of(),
                characteristics,
                characteristicsRu,
                productType,
                minCustomWeight,
                allowCustomWeight,
                weightOptions,
                groupId,
                variantName,
                variantValue
        );
    }

    private BigDecimal parsePrice(String raw, String fieldLabel, boolean required) {
        if (raw == null || raw.isBlank()) {
            if (required) {
                throw new RowValidationException("Не вказано поле '" + fieldLabel + "'");
            }
            return BigDecimal.ZERO;
        }
        String cleaned = raw.replace("грн", "")
                .replace("₴", "")
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace(",", ".")
                .trim();
        try {
            BigDecimal val = new BigDecimal(cleaned);
            if (val.compareTo(BigDecimal.ZERO) < 0 || val.compareTo(BigDecimal.valueOf(100000)) > 0) {
                throw new RowValidationException("Поле '" + fieldLabel + "' має бути в межах від 0 до 100000");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Некоректне числове значення для поля '" + fieldLabel + "': " + raw);
        }
    }

    private Integer parseDiscount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String cleaned = raw.replace("%", "").replace(",", ".").trim();
        try {
            int val = new BigDecimal(cleaned).intValue();
            if (val < 0 || val > 99) {
                throw new RowValidationException("Знижка має бути від 0 до 99%");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Некоректне значення знижки: " + raw);
        }
    }

    private ProductCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RowValidationException("Не вказано категорію товару");
        }
        String normalized = raw.trim();

        for (ProductCategory cat : ProductCategory.values()) {
            if (cat.name().equalsIgnoreCase(normalized) || cat.getTitle().equalsIgnoreCase(normalized)) {
                return cat;
            }
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "суперфуд", "суперфуди", "superfoods", "superfood" -> ProductCategory.SUPER_FOOD;
            case "олія та масла", "олії та масла", "олії", "олія", "масла" -> ProductCategory.OIL_AND_BUTTERS;
            case "снеки", "снеки і чіпси", "чіпси" -> ProductCategory.SNACKS_AND_CHIPS;
            case "сухофрукт" -> ProductCategory.DRIED_FRUITS;
            case "горіх", "горішки" -> ProductCategory.NUTS;
            case "цукерка" -> ProductCategory.CANDIES;
            case "спеція", "приправи" -> ProductCategory.SPICES;
            default -> throw new RowValidationException(
                    "Невідома категорія '" + raw + "'. Допустимі: Сухофрукти, Горіхи, Солодощі, Цукерки, Суперфуди, Олії та масла, Консервація, Чай, Кава, Снеки та чіпси, Спеції"
            );
        };
    }

    private ProductType parseProductType(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProductType.SIMPLE;
        }
        String normalized = raw.trim();
        for (ProductType pt : ProductType.values()) {
            if (pt.name().equalsIgnoreCase(normalized)) {
                return pt;
            }
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("звичайний") || lower.contains("простий") || lower.equals("simple")) {
            return ProductType.SIMPLE;
        }
        if (lower.contains("ваг") || lower.contains("фасуван") || lower.equals("weight")) {
            return ProductType.WEIGHT_BASED;
        }
        if (lower.contains("варіац") || lower.contains("варіант") || lower.contains("груп") || lower.equals("variant")) {
            return ProductType.VARIANT;
        }

        throw new RowValidationException(
                "Невідомий тип товару '" + raw + "'. Допустимі: Звичайний (SIMPLE), На вагу (WEIGHT_BASED), Варіація (VARIANT)"
        );
    }

    private Integer parseMeasureValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RowValidationException("Не вказано значення розміру (вага/об'єм)");
        }
        String cleaned = raw.replaceAll("[^0-9.,-]", "").replace(",", ".").trim();
        if (cleaned.isEmpty()) {
            throw new RowValidationException("Некоректне значення розміру: " + raw);
        }
        try {
            int val = new BigDecimal(cleaned).intValue();
            if (val < 0 || val > 999) {
                throw new RowValidationException("Значення розміру має бути від 0 до 999 (отримано: " + val + ")");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Некоректне числове значення розміру: " + raw);
        }
    }

    private String parseMeasureUnit(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RowValidationException("Не вказано одиницю розміру");
        }
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replace(".", "");
        cleaned = switch (cleaned) {
            case "гр", "грам", "грами", "g", "gr" -> "г";
            case "кілограм", "кг", "kg" -> "кг";
            case "мілілітр", "мл", "ml" -> "мл";
            case "літр", "л", "l" -> "л";
            case "штук", "штука", "шт", "pcs" -> "шт";
            case "сантиметр", "см", "cm" -> "см";
            case "метр", "м", "m" -> "м";
            default -> cleaned;
        };

        if (!VALID_UNIT_PATTERN.matcher(cleaned).matches()) {
            throw new RowValidationException(
                    "Недопустима одиниця розміру '" + raw + "'. Допустимі: г, кг, мл, л, см, м, шт, -"
            );
        }
        return cleaned;
    }

    private Map<String, String> parseCharacteristics(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return map;
        }

        String[] entries = raw.split("[;\\r\\n]+");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int sepIdx = trimmed.indexOf(':');
            if (sepIdx < 0) {
                sepIdx = trimmed.indexOf('=');
            }
            if (sepIdx > 0 && sepIdx < trimmed.length() - 1) {
                String key = trimmed.substring(0, sepIdx).trim();
                String val = trimmed.substring(sepIdx + 1).trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    map.put(key, val);
                }
            }
        }
        return map;
    }

    private String buildAttributesString(Map<String, String> characteristics, String rawCharacteristics) {
        if (!characteristics.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : characteristics.entrySet()) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            return sb.toString();
        }
        return rawCharacteristics != null ? rawCharacteristics.trim() : "";
    }

    private List<ProductWeightOptionDTO> parseWeightOptions(String raw, BigDecimal basePrice) {
        List<ProductWeightOptionDTO> options = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            String[] parts = raw.split("[;\\r\\n]+");
            boolean hasExplicitDefault = false;

            for (String part : parts) {
                String item = part.trim();
                if (item.isEmpty()) {
                    continue;
                }
                boolean isDefault = item.endsWith("*");
                if (isDefault) {
                    item = item.substring(0, item.length() - 1).trim();
                }

                int sepIdx = item.indexOf(':');
                if (sepIdx < 0) {
                    sepIdx = item.indexOf('=');
                }
                if (sepIdx < 0) {
                    sepIdx = item.indexOf('-');
                }
                if (sepIdx <= 0 || sepIdx >= item.length() - 1) {
                    throw new RowValidationException(
                            "Некоректний формат фасування '" + part.trim() + "'. Очікується формат 'вага:ціна', напр. '100:55; 250:130; 1000:480*'"
                    );
                }

                String weightStr = item.substring(0, sepIdx).replaceAll("[^0-9]", "").trim();
                String priceStr = item.substring(sepIdx + 1).replace("грн", "").replace("₴", "").replace(",", ".").trim();

                try {
                    int weightVal = Integer.parseInt(weightStr);
                    BigDecimal optPrice = new BigDecimal(priceStr);
                    if (weightVal <= 0) {
                        throw new RowValidationException("Вага фасування має бути додатною: " + part);
                    }
                    if (optPrice.compareTo(BigDecimal.ZERO) < 0) {
                        throw new RowValidationException("Ціна фасування не може бути від'ємною: " + part);
                    }
                    if (isDefault) {
                        hasExplicitDefault = true;
                    }
                    options.add(new ProductWeightOptionDTO(null, weightVal, optPrice, isDefault, true));
                } catch (NumberFormatException e) {
                    throw new RowValidationException("Некоректні числа у фасуванні '" + part.trim() + "'");
                }
            }

            if (!options.isEmpty() && !hasExplicitDefault) {
                int defaultIdx = 0;
                for (int i = 0; i < options.size(); i++) {
                    if (Integer.valueOf(1000).equals(options.get(i).weightValue())) {
                        defaultIdx = i;
                        break;
                    }
                }
                ProductWeightOptionDTO chosen = options.get(defaultIdx);
                options.set(defaultIdx, new ProductWeightOptionDTO(null, chosen.weightValue(), chosen.price(), true, true));
            }
        }

        if (options.isEmpty()) {
            options.add(new ProductWeightOptionDTO(null, 1000, basePrice, true, true));
        }

        return options;
    }

    private boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        return lower.equals("так") || lower.equals("yes") || lower.equals("true") || lower.equals("1") || lower.equals("+");
    }

    private Integer parseOptionalPositiveInt(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9-]", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            int val = Integer.parseInt(cleaned);
            if (val < 0) {
                throw new RowValidationException(fieldLabel + " не може бути від'ємною");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Некоректне значення поля '" + fieldLabel + "': " + raw);
        }
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private boolean isHeaderRow(ExcelRow row) {
        for (String cell : row.cells()) {
            if (cell == null) continue;
            String lower = cell.trim().toLowerCase(Locale.ROOT);
            if (lower.startsWith("назва") || lower.startsWith("ціна") || lower.startsWith("категорія") || lower.equals("name") || lower.equals("price")) {
                return true;
            }
        }
        return false;
    }

    private static class RowValidationException extends RuntimeException {
        public RowValidationException(String message) {
            super(message);
        }
    }

    private static class ColumnMap {
        int nameIdx = -1;
        int priceIdx = -1;
        int purchasePriceIdx = -1;
        int discountIdx = -1;
        int characteristicsIdx = -1;
        int manufacturerIdx = -1;
        int productTypeIdx = -1;
        int categoryIdx = -1;
        int subcategoryIdx = -1;
        int valueOfMeasureIdx = -1;
        int unitOfMeasureIdx = -1;
        int weightOptionsIdx = -1;
        int allowCustomWeightIdx = -1;
        int minCustomWeightIdx = -1;
        int groupIdIdx = -1;
        int variantNameIdx = -1;
        int variantValueIdx = -1;

        static ColumnMap fromHeaderRow(ExcelRow headerRow) {
            ColumnMap map = new ColumnMap();
            List<String> cells = headerRow.cells();

            for (int i = 0; i < cells.size(); i++) {
                String raw = cells.get(i);
                if (raw == null || raw.isBlank()) continue;
                String h = raw.trim().toLowerCase(Locale.ROOT);

                if (h.startsWith("назва параметра") || h.equals("параметр варіації") || h.equals("variantname") || h.equals("variant_name")) {
                    map.variantNameIdx = i;
                } else if (h.startsWith("значення варіації") || h.equals("значення параметра") || h.equals("variantvalue") || h.equals("variant_value")) {
                    map.variantValueIdx = i;
                } else if (h.startsWith("значення розміру") || h.startsWith("значення (вага") || h.equals("значення") || h.equals("valueofinitofmeasure")) {
                    map.valueOfMeasureIdx = i;
                } else if (h.startsWith("одиниця") || h.equals("initofmeasure") || h.equals("unit")) {
                    map.unitOfMeasureIdx = i;
                } else if (h.startsWith("назва") || h.equals("name") || h.equals("товар")) {
                    if (map.nameIdx == -1) map.nameIdx = i;
                } else if (h.startsWith("закуп") || h.equals("purchaseprice") || h.equals("purchase_price")) {
                    map.purchasePriceIdx = i;
                } else if (h.startsWith("ціна") || h.startsWith("базова ціна") || h.equals("price")) {
                    if (map.priceIdx == -1) map.priceIdx = i;
                } else if (h.startsWith("знижка") || h.equals("discount")) {
                    map.discountIdx = i;
                } else if (h.startsWith("характеристик") || h.startsWith("характиристик") || h.equals("characteristics") || h.equals("attributes")) {
                    map.characteristicsIdx = i;
                } else if (h.startsWith("підкатегорія") || h.equals("subcategory")) {
                    map.subcategoryIdx = i;
                } else if (h.startsWith("категорія") || h.equals("category") || h.equals("productcategory")) {
                    map.categoryIdx = i;
                } else if (h.startsWith("виробник") || h.equals("manufacturer") || h.equals("brand")) {
                    if (map.manufacturerIdx == -1) {
                        map.manufacturerIdx = i;
                    } else if (map.subcategoryIdx == -1) {
                        map.subcategoryIdx = i;
                    }
                } else if (h.startsWith("тип") || h.equals("producttype") || h.equals("product_type")) {
                    map.productTypeIdx = i;
                } else if (h.startsWith("фасування") || h.startsWith("вагові опції") || h.startsWith("фіксовані фасування") || h.equals("weightoptions")) {
                    map.weightOptionsIdx = i;
                } else if (h.startsWith("дозволити власну вагу") || h.startsWith("власна вага") || h.equals("allowcustomweight")) {
                    map.allowCustomWeightIdx = i;
                } else if (h.startsWith("мін. кастомна вага") || h.startsWith("мінімальна вага") || h.equals("mincustomweight")) {
                    map.minCustomWeightIdx = i;
                } else if (h.startsWith("group id") || h.equals("groupid") || h.equals("group_id") || h.startsWith("група")) {
                    map.groupIdIdx = i;
                }
            }
            return map;
        }

        static ColumnMap defaultPositional(int columnCount) {
            ColumnMap map = new ColumnMap();
            if (columnCount <= 9) {
                // Порядок із запиту користувача:
                // 0: назва (укр), 1: ціна, 2: характеристика, 3: виробник, 4: тип товару,
                // 5: категорія (укр), 6: підкатегорія (укр), 7: значення розміру, 8: одиниця розміру
                map.nameIdx = 0;
                map.priceIdx = 1;
                map.characteristicsIdx = 2;
                map.manufacturerIdx = 3;
                map.productTypeIdx = 4;
                map.categoryIdx = 5;
                map.subcategoryIdx = 6;
                map.valueOfMeasureIdx = 7;
                map.unitOfMeasureIdx = 8;
            } else {
                // Порядок шаблону з розширеними колонками
                map.nameIdx = 0;
                map.priceIdx = 1;
                map.purchasePriceIdx = 2;
                map.discountIdx = 3;
                map.characteristicsIdx = 4;
                map.manufacturerIdx = 5;
                map.productTypeIdx = 6;
                map.categoryIdx = 7;
                map.subcategoryIdx = 8;
                map.valueOfMeasureIdx = 9;
                map.unitOfMeasureIdx = 10;
                map.weightOptionsIdx = 11;
                map.allowCustomWeightIdx = 12;
                map.minCustomWeightIdx = 13;
                map.groupIdIdx = 14;
                map.variantNameIdx = 15;
                map.variantValueIdx = 16;
            }
            return map;
        }
    }
}
