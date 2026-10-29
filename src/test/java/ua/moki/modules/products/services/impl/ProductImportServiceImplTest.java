package ua.moki.modules.products.services.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ua.moki.infrastructure.storage.dtos.TranslatorDTO;
import ua.moki.infrastructure.storage.service.GeminiProductGenerator;
import ua.moki.infrastructure.storage.service.TranslatorService;
import ua.moki.modules.products.dtos.ProductImportResultDTO;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.enums.ProductType;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.ProductService;
import ua.moki.modules.products.utils.ExcelWorkbookHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceImplTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private GeminiProductGenerator geminiProductGenerator;

    @Mock
    private TranslatorService translatorService;

    private ProductImportServiceImpl productImportService;

    @BeforeEach
    void setUp() {
        productImportService = new ProductImportServiceImpl(
                productService,
                productRepository,
                geminiProductGenerator,
                translatorService
        );
    }

    @Test
    @DisplayName("generateTemplate creates valid xlsx with expected headers")
    void generateTemplate_shouldCreateValidXlsxWithHeaders() throws Exception {
        byte[] templateBytes = productImportService.generateTemplate();
        assertThat(templateBytes).isNotEmpty();

        List<ExcelWorkbookHelper.ExcelRow> rows = ExcelWorkbookHelper.readFirstSheet(new ByteArrayInputStream(templateBytes));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getCell(0)).isEqualTo("Назва (укр)");
        assertThat(rows.getFirst().getCell(1)).isEqualTo("Ціна");
    }

    @Test
    @DisplayName("importProductsFromExcel imports valid rows, generates SEO description & RU translation, and skips duplicates")
    void importProductsFromExcel_shouldImportValidRowsAndSkipDuplicatesAndInvalidRows() throws Exception {
        byte[] templateBytes = productImportService.generateTemplate();
        byte[] testXlsx = promoteSecondSheetToFirst(templateBytes);

        when(productRepository.existsBySlug("manho-sushene-naturalne-royal")).thenReturn(false);
        when(productRepository.existsBySlug("myhdal-zolotyi-syryi")).thenReturn(true);
        when(productRepository.existsBySlug("tsukerky-chornoslyv-z-voloskym-horikhom-250h")).thenReturn(false);

        when(geminiProductGenerator.generateDescription(any(), any()))
                .thenReturn("Згенерований SEO опис товару");

        when(translatorService.translateTextsToRu(any())).thenAnswer(inv -> {
            TranslatorDTO dto = inv.getArgument(0);
            return new TranslatorDTO(
                    dto.name() + " (RU)",
                    dto.subcategory() + " (RU)",
                    dto.description() + " (RU)",
                    Map.of("Страна", "Украина")
            );
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                testXlsx
        );

        ProductImportResultDTO result = productImportService.importProductsFromExcel(file);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst().rowNumber()).isEqualTo(3);
        assertThat(result.errors().getFirst().productName()).isEqualTo("Мигдаль золотий сирий");

        ArgumentCaptor<ProductRequestDTO> captor = ArgumentCaptor.forClass(ProductRequestDTO.class);
        verify(productService, times(2)).createProduct(captor.capture());

        List<ProductRequestDTO> created = captor.getAllValues();
        ProductRequestDTO first = created.get(0);
        assertThat(first.name()).isEqualTo("Манго сушене натуральне Royal");
        assertThat(first.nameRu()).isEqualTo("Манго сушене натуральне Royal (RU)");
        assertThat(first.availability()).isEqualTo(ProductAvailability.OUT_OF_STOCK);
        assertThat(first.productCategory()).isEqualTo(ProductCategory.DRIED_FRUITS);
        assertThat(first.productType()).isEqualTo(ProductType.SIMPLE);
        assertThat(first.price()).isEqualByComparingTo(BigDecimal.valueOf(220));
        assertThat(first.purchasePrice()).isEqualByComparingTo(BigDecimal.valueOf(160));
        assertThat(first.description()).isEqualTo("Згенерований SEO опис товару");
        assertThat(first.descriptionRu()).isEqualTo("Згенерований SEO опис товару (RU)");
        assertThat(first.images()).isEmpty();

        ProductRequestDTO second = created.get(1);
        assertThat(second.productType()).isEqualTo(ProductType.VARIANT);
        assertThat(second.groupId()).isEqualTo("amanti-prunes-walnut");
        assertThat(second.variantName()).isEqualTo("Вага");
        assertThat(second.variantValue()).isEqualTo("250г");
        assertThat(second.availability()).isEqualTo(ProductAvailability.OUT_OF_STOCK);

        verify(geminiProductGenerator).generateDescription(
                eq("Манго сушене натуральне Royal"),
                eq("Країна: В'єтнам, Склад: 100% манго, Без цукру: Так")
        );
    }

    @Test
    @DisplayName("importProductsFromExcel supports 9-column positional Excel without header row and falls back when Gemini returns empty")
    void importProductsFromExcel_shouldSupport9ColumnPositionalLayoutAndGeminiFallback() throws Exception {
        byte[] xlsxBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            // 0: назва (укр), 1: ціна, 2: характеристика, 3: виробник, 4: тип товару,
            // 5: категорія (укр), 6: підкатегорія (укр), 7: значення розміру, 8: одиниця розміру
            row.createCell(0).setCellValue("Кеш'ю смажений");
            row.createCell(1).setCellValue("520 грн");
            row.createCell(2).setCellValue("Країна: Індія; Обробка: Смажений");
            row.createCell(3).setCellValue("Moki Nuts");
            row.createCell(4).setCellValue("На вагу");
            row.createCell(5).setCellValue("Горіхи");
            row.createCell(6).setCellValue("Кеш'ю");
            row.createCell(7).setCellValue("1");
            row.createCell(8).setCellValue("кг");
            wb.write(baos);
            xlsxBytes = baos.toByteArray();
        }

        when(productRepository.existsBySlug(any())).thenReturn(false);
        when(geminiProductGenerator.generateDescription(any(), any())).thenReturn("");
        when(translatorService.translateTextsToRu(any())).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "positional.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes
        );

        ProductImportResultDTO result = productImportService.importProductsFromExcel(file);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();

        ArgumentCaptor<ProductRequestDTO> captor = ArgumentCaptor.forClass(ProductRequestDTO.class);
        verify(productService).createProduct(captor.capture());

        ProductRequestDTO dto = captor.getValue();
        assertThat(dto.name()).isEqualTo("Кеш'ю смажений");
        assertThat(dto.nameRu()).isEqualTo("Кеш'ю смажений");
        assertThat(dto.productCategory()).isEqualTo(ProductCategory.NUTS);
        assertThat(dto.productType()).isEqualTo(ProductType.WEIGHT_BASED);
        assertThat(dto.availability()).isEqualTo(ProductAvailability.OUT_OF_STOCK);
        assertThat(dto.description()).contains("Кеш'ю смажений. Характеристики: Країна: Індія, Обробка: Смажений");
        assertThat(dto.weightOptions()).hasSize(1);
        assertThat(dto.weightOptions().getFirst().weightValue()).isEqualTo(1000);
        assertThat(dto.weightOptions().getFirst().price()).isEqualByComparingTo("520");
    }

    @Test
    @DisplayName("importProductsFromExcel throws IllegalArgumentException when file is empty")
    void importProductsFromExcel_shouldThrowException_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);

        assertThatThrownBy(() -> productImportService.importProductsFromExcel(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("порожній");
    }

    private byte[] promoteSecondSheetToFirst(byte[] xlsxBytes) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.removeSheetAt(0);
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}
