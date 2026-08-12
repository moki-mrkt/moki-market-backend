package ua.moki.modules.products.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.yml_catalog.*;
import ua.moki.modules.products.dtos.yml_catalog.param.ComplexParam;
import ua.moki.modules.products.dtos.yml_catalog.param.Param;
import ua.moki.modules.products.dtos.yml_catalog.param.SimpleParam;
import ua.moki.modules.products.dtos.yml_catalog.prom.PromOffer;
import ua.moki.modules.products.dtos.yml_catalog.prom.PromParam;
import ua.moki.modules.products.dtos.yml_catalog.prom.PromShop;
import ua.moki.modules.products.dtos.yml_catalog.prom.PromYmlCatalog;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.YmlExportService;

import javax.xml.catalog.Catalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class YmlExportServiceImpl implements YmlExportService {

    private final ProductRepository productRepository;

    private final XmlMapper xmlMapper = XmlMapper.builder()
            .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
            .build();

    @Value("${s3.public_url}")
    private String storageUrl;

    @Transactional(readOnly = true)
    public String generateYmlForCandiesByRozetka() {
        return generateYml(
                List.of(new Category("101", "4629506",null, "Цукерки")),
                this::buildRozetkaParams
        );
    }

    @Transactional(readOnly = true)
    public String generateYmlForCandiesByKasta() {
        return generateYml(
                List.of(new Category("101", "4629506", null, "Цукерки у коробці")),
                this::buildKastaParams
        );
    }

    @Transactional(readOnly = true)
    public String generateYmlForProm() {

        List<Currency> currencies = List.of(
                new Currency("UAH", "1"),
                new Currency("USD", "44.6"),
                new Currency("EUR", "51.2")
        );

        // Якщо маєте portal_id від Prom.ua (наприклад, 12345), вкажіть його тут другим параметром
        List<Category> categories = List.of(
                new Category("101", null, "12345", "Цукерки")
        );

        List<Product> products = productRepository
                .findAllByProductCategory(ProductCategory.SWEETS, Pageable.unpaged())
                .getContent();

        List<PromOffer> offers = products.stream()
                .map(p -> mapToPromOffer(p, "101"))
                .toList();

        PromShop shop = new PromShop(
                "Moki Market",
                "Moki",
                "https://moki.com.ua/",
                currencies,
                categories,
                offers
        );

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        PromYmlCatalog catalog = new PromYmlCatalog(date, shop);

        try {
            return xmlMapper.writeValueAsString(catalog);
        } catch (JsonProcessingException e) {
            log.error("Помилка генерації Prom YML", e);
            throw new RuntimeException("Не вдалося згенерувати Prom YML", e);
        }
    }

    private String generateYml(List<Category> categories, Function<Product, List<Param>> paramBuilder) {
        List<Product> products = productRepository
                .findAllByProductCategory(ProductCategory.CANDIES, Pageable.unpaged())
                .getContent();

        List<Currency> currencies = List.of(
                new Currency("UAH", "1"),
                new Currency("USD", "44.6"),
                new Currency("EUR", "51.2")
        );

        List<Offer> offers = products.stream()
                .map(p -> mapToOffer(p, "101", paramBuilder.apply(p)))
                .toList();

        Shop shop = new Shop("Moki Market", "Moki", "https://moki.com.ua/", currencies, categories, offers);

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        YmlCatalog catalog = new YmlCatalog(date, shop);

        try {
            return xmlMapper.writeValueAsString(catalog);
        } catch (JsonProcessingException e) {
            log.error("Помилка генерації XML", e);
            throw new RuntimeException("Не вдалося згенерувати YML фід", e);
        }
    }

    private Offer mapToOffer(Product product, String categoryId, List<Param> params) {
        BigDecimal priceForRozetka = calculatePrice(product);

        List<String> pictures = product.getImages().stream()
                .map(image -> storageUrl + image.getImageId() + "_large.webp")
                .toList();

        return new Offer(
                String.valueOf(product.getId()),
                true,
                priceForRozetka,
                priceForRozetka.add(BigDecimal.TEN),
                priceForRozetka.subtract(BigDecimal.TEN),
                String.valueOf(product.getId() + 100),
                100,
                "https://moki.com.ua/products/" + product.getSlug(),
                "UAH",
                categoryId,
                pictures,
                product.getManufacturerOfTheProduct(),
                product.getName(),
                product.getDescription(),
                params
        );
    }

    private PromOffer mapToPromOffer(Product product, String categoryId) {
        // Логіка розрахунку ціни
        BigDecimal basePrice = calculatePrice(product);
        BigDecimal oldPrice = null;
        BigDecimal currentPrice = basePrice;

        // Відповідно до інструкції Prom: якщо є знижка, price - це ціна зі знижкою, oldprice - ціна без знижки
        if (product.getDiscount() != null && product.getDiscount() > 0) {
            currentPrice = basePrice.subtract(BigDecimal.TEN); // Ваша логіка з відніманням 10
            oldPrice = basePrice;
        }

        // Обмеження Prom.ua до 10 фото
        List<String> pictures = product.getImages().stream()
                .limit(10)
                .map(image -> storageUrl + image.getImageId() + "_large.webp")
                .toList();

        // Визначення доступності
        String available = product.getAvailability() == ProductAvailability.IN_STOCK ? "true" : "false";

        // Формування параметрів
        List<PromParam> params = List.of(
                new PromParam("Гарантія", null, "1 місяць"),
                new PromParam("Вага", product.getInitOfMeasure(), String.valueOf(product.getValueOfInitOfMeasure())),
                new PromParam("Країна виробник", null, "Україна")
        );

        return new PromOffer(
                String.valueOf(product.getId()),
                available,
                "true", // in_stock="true" (готово до відправки)
                "r",    // selling_type="r" (тільки в роздріб)
                product.getName(),
                product.getName(), // name_ua (Тут підставте укр назву, якщо вона зберігається окремо)
                categoryId,
                currentPrice,
                oldPrice,
                "UAH",
                pictures,
                product.getManufacturerOfTheProduct(),
                String.valueOf(product.getId() + 100), // Артикул
                product.getDescription(),
                product.getDescription(), // description_ua
                params
        );
    }

    private List<Param> buildRozetkaParams(Product p) {
        return List.of(
                new ComplexParam("Гарантія", "20769", "11", List.of(
                        new ParamValue("uk", "1 місяць"), new ParamValue("ru", "1 месяц")
                )),
                new ComplexParam("Вага в упаковці, " + p.getInitOfMeasure(), "48739", "12", List.of(
                        new ParamValue("uk", String.valueOf(p.getValueOfInitOfMeasure())),
                        new ParamValue("ru", String.valueOf(p.getValueOfInitOfMeasure()))
                )),
                new ComplexParam("Термін зберігання", "71434", "13", List.of(
                        new ParamValue("uk", "6 місяців"), new ParamValue("ru", "6 месяцев")
                )),
                new ComplexParam("Вес, %s".formatted(p.getInitOfMeasure()), "147016", "15", List.of(
                        new ParamValue("uk", p.getValueOfInitOfMeasure().toString()),
                        new ParamValue("ru", p.getValueOfInitOfMeasure().toString())
                )),
                new ComplexParam("Різновид", "", "16", List.of(
                        new ParamValue("uk", "Десерт"),
                        new ParamValue("ru", "Десерт")
                )),
                new ComplexParam("Упаковка", "147152", "17", List.of(
                        new ParamValue("uk", "Картонна коробка"),
                        new ParamValue("ru", "Картонная коробка")
                )),
                new ComplexParam("Умови зберігання", "137119", "18", List.of(
                        new ParamValue("uk", "У сухому місці"),
                        new ParamValue("ru", "В сухом месте")
                )),
                new ComplexParam("Тип шоколаду", "137119", "19", List.of(
                        new ParamValue("uk", "Молочний"),
                        new ParamValue("ru", "Молочный")
                )),
                new ComplexParam("Країна-виробник товару", "98900", "20", List.of(
                        new ParamValue("uk", "Україна"),
                        new ParamValue("ru", "Украина")
                )),
                new ComplexParam("Кількість вантажних місць", "72961", "21", List.of(
                        new ParamValue("uk", "1"),
                        new ParamValue("ru", "1")
                )),
                new SimpleParam("Підкатегорія", p.getSubcategory())
        );
    }

    private List<Param> buildKastaParams(Product p) {
        return List.of(
                new SimpleParam("Колір", "-"),
                new SimpleParam("Розмір", "-"),
                new SimpleParam("Габарити в упаковці,см", "29х19х6"),
                new SimpleParam("Вага, " + p.getInitOfMeasure(), String.valueOf(p.getValueOfInitOfMeasure())),
                new SimpleParam("Країна виробник", "Україна")
        );
    }

//    @Transactional(readOnly = true)
//    public String generateYmlForCandies() {
//
//        List<Product> products = productRepository
//                .findAllByProductCategory(
//                        ProductCategory.CANDIES,
//                        Pageable.unpaged()
//                ).getContent();
//
//        StringBuilder xml = new StringBuilder();
//
//        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//        xml.append("<yml_catalog date=\"")
//                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
//                .append("\">\n");
//
//        xml.append("    <shop>\n");
//        xml.append("        <name>Moki Market</name>\n");
//        xml.append("        <company>Moki</company>\n");
//        xml.append("        <url>https://moki.com.ua/</url>\n");
//
//        xml.append("        <currencies>\n");
//        xml.append("            <currency id=\"UAH\" rate=\"1\"/>\n");
//        xml.append("            <currency id=\"USD\" rate=\"44.6\"/>\n");
//        xml.append("            <currency id=\"EUR\" rate=\"51.2\"/>\n");
//        xml.append("        </currencies>\n");
//
//        xml.append("        <categories>\n");
//        xml.append("            <category id=\"101\">Цукерки</category>\n");
//        xml.append("        </categories>\n");
//
//        xml.append("        <offers>\n");
//
//        for (Product product : products) {
//            xml.append("            <offer id=\"").append(product.getId()).append("\" available=\"true\">\n");
//
//           BigDecimal priceForRozetka = calculatePrice(product);
//           xml.append("                <price>").append(priceForRozetka).append("</price>\n");
//           xml.append("                <price_old>").append(priceForRozetka.add(BigDecimal.TEN)).append("</price_old>\n");
//           xml.append("                <price_promo>").append(priceForRozetka.subtract(BigDecimal.TEN)).append("</price_promo>\n");
//
//           xml.append("                <article>").append(product.getId() + 100).append("</article>\n");
//           xml.append("                <stock_quantity>100</stock_quantity>\n");
//           xml.append("                <url>").append("https://moki.com.ua/products/").append(product.getSlug()).append("</url>\n");
//           xml.append("                <currencyId>UAH</currencyId>\n");
//           xml.append("                <categoryId>101</categoryId>\n"); // Відповідає category id вище
//
//            for (ProductImage image : product.getImages()) {
//                String imageUrl = storageUrl + image.getImageId() + "_large.webp";
//                xml.append("                <picture>").append(imageUrl).append("</picture>\n");
//            }
//
//            xml.append("                <vendor>").append(product.getManufacturerOfTheProduct()).append("</vendor>\n");
//            xml.append("                <name>").append(product.getName()).append("</name>\n");
//            xml.append("                <description>").append(product.getDescription()).append("</description>\n");
//
//            xml.append("""
//                                        <param name="Гарантія" paramid="20769" valueid="11">
//                                            <value lang="uk">1 місяць</value>
//                                            <value lang="ru">1 месяц</value>
//                                        </param>
//                    """);
//
//            xml.append("""
//                                        <param name="Вага в упаковці, %s" paramid="48739" valueid="12">
//                                            <value lang="uk">%s</value>
//                                            <value lang="ru">%s</value>
//                                        </param>
//                    """.formatted(product.getInitOfMeasure(), product.getValueOfInitOfMeasure(), product.getValueOfInitOfMeasure()));
//
//            xml.append("""
//                                        <param name="Срок хранения" paramid="71434" valueid="13">
//                                            <value lang="uk">6 місяців</value>
//                                            <value lang="ru">6 месяцев</value>
//                                        </param>
//                    """);
//
//            xml.append("""
//                                        <param name="Вес, %s" paramid="147016" valueid="15">
//                                            <value lang="uk">%s</value>
//                                            <value lang="ru">%s</value>
//                                        </param>
//                    """.formatted(product.getInitOfMeasure(), product.getValueOfInitOfMeasure(), product.getValueOfInitOfMeasure()));
//
//            xml.append("""
//                                        <param name="Вид" paramid="" valueid="16">
//                                            <value lang="uk">Десерт</value>
//                                            <value lang="ru">Десерт</value>
//                                        </param>
//                    """);
//
//            xml.append("""
//                                        <param name="Упаковка" paramid="147152" valueid="17">
//                                            <value lang="uk">Картонна коробка</value>
//                                            <value lang="ru">Картонная коробка</value>
//                                        </param>
//                    """);
//
//            xml.append("""
//                                        <param name="Условия хранения" paramid="137119" valueid="18">
//                                            <value lang="uk">У сухому місці</value>
//                                            <value lang="ru"> В сухом месте</value>
//                                        </param>
//                    """);
//
//            xml.append("""
//                                        <param name="Тип шоколада" paramid="" valueid="17">
//                                            <value lang="uk">Молочний</value>
//                                            <value lang="ru">Молочный</value>
//                                        </param>
//                    """);
//
//            xml.append("""
//                                        <param name="Страна-производитель товара" paramid="98900" valueid="18">
//                                            <value lang="uk">Україна</value>
//                                            <value lang="ru">Украина</value>
//                                        </param>
//                    """);
//
//            xml.append("""
//                                        <param name="Количество грузовых мест" paramid="72961" valueid="19">
//                                            <value lang="uk">1</value>
//                                            <value lang="ru">1</value>
//                                        </param>
//                    """);
//
//            xml.append("                <param name=\"Підкатегорія\">").append(product.getSubcategory()).append("</param>\n");
//
//            xml.append("            </offer>\n");
//        }
//
//        xml.append("        </offers>\n");
//        xml.append("    </shop>\n");
//        xml.append("</yml_catalog>");
//
//        return xml.toString();
//    }
//
//    @Transactional(readOnly = true)
//    public String generateYmlForCandiesByKasta() {
//        List<Product> products = productRepository
//                .findAllByProductCategory(
//                        ProductCategory.CANDIES,
//                        Pageable.unpaged()
//                ).getContent();
//
//        StringBuilder xml = new StringBuilder();
//
//        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//        xml.append("<yml_catalog date=\"")
//                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
//                .append("\">\n");
//
//        xml.append("    <shop>\n");
//        xml.append("        <name>Moki Market</name>\n");
//        xml.append("        <company>Moki</company>\n");
//        xml.append("        <url>https://moki.com.ua/</url>\n");
//
//        xml.append("        <currencies>\n");
//        xml.append("            <currency id=\"UAH\" rate=\"1\"/>\n");
//        xml.append("            <currency id=\"USD\" rate=\"44.6\"/>\n");
//        xml.append("            <currency id=\"EUR\" rate=\"51.2\"/>\n");
//        xml.append("        </currencies>\n");
//
//        xml.append("        <categories>\n");
//        xml.append("            <category id=\"101\" rz_id=\"4629506\" >Цукерки у коробці</category>\n");
//        xml.append("        </categories>\n");
//
//        xml.append("        <offers>\n");
//
//        for (Product product : products) {
//            xml.append("            <offer id=\"").append(product.getId()).append("\" available=\"true\">\n");
//
//            BigDecimal priceForRozetka = calculatePrice(product);
//            xml.append("                <price>").append(priceForRozetka).append("</price>\n");
//            xml.append("                <price_old>").append(priceForRozetka.add(BigDecimal.TEN)).append("</price_old>\n");
//            xml.append("                <price_promo>").append(priceForRozetka.subtract(BigDecimal.TEN)).append("</price_promo>\n");
//
//            xml.append("                <article>").append(product.getId() + 100).append("</article>\n");
//            xml.append("                <stock_quantity>100</stock_quantity>\n");
//            xml.append("                <url>").append("https://moki.com.ua/products/").append(product.getSlug()).append("</url>\n");
//            xml.append("                <currencyId>UAH</currencyId>\n");
//            xml.append("                <categoryId>101</categoryId>\n");
//
//            for (ProductImage image : product.getImages()) {
//                String imageUrl = storageUrl + image.getImageId() + "_large.webp";
//                xml.append("                <picture>").append(imageUrl).append("</picture>\n");
//            }
//
//            xml.append("                <vendor>").append(product.getManufacturerOfTheProduct()).append("</vendor>\n");
//            xml.append("                <name>").append(product.getName()).append("</name>\n");
//            xml.append("                <description>").append(product.getDescription()).append("</description>\n");
//
//            xml.append("""
//                                        <param name="Колір">-</param>
//                                        <param name="Розмір">-</param>
//                                        <param name="Габарити в упаковці,см">29х19х6</param>
//                                        <param name="Вага, %s">%s</param>
//                                        <param name="Країна виробник">Україна</param>
//                    """.formatted(product.getInitOfMeasure(), product.getValueOfInitOfMeasure()));
//
//            xml.append("            </offer>\n");
//        }
//
//        xml.append("        </offers>\n");
//        xml.append("    </shop>\n");
//        xml.append("</yml_catalog>");
//
//        return xml.toString();
//    }

    private BigDecimal calculatePrice(Product product) {

        BigDecimal initialPrice = product.getPrice()
                .multiply(new BigDecimal("1.165"))
                .setScale(0, RoundingMode.HALF_UP);
        
        BigDecimal purchasePrice = product.getPurchasePrice();
        
        BigDecimal priceMinus16Percent = initialPrice.multiply(new BigDecimal("0.84"));

        BigDecimal profit = priceMinus16Percent.subtract(purchasePrice);

        long currentVal = initialPrice.longValue();
        long remainder = currentVal % 10;
        long finalPriceValue;

        if (profit.compareTo(new BigDecimal("35")) < 0) {
            finalPriceValue = currentVal + (9 - remainder);
        } else {
            if (remainder == 9) {
                finalPriceValue = currentVal;
            } else {
                finalPriceValue = currentVal - remainder - 1;
            }
        }

        return BigDecimal.valueOf(finalPriceValue);
    }
}
