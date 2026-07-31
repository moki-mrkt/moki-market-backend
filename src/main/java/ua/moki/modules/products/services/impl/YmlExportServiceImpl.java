package ua.moki.modules.products.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;
import ua.moki.modules.products.services.YmlExportService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YmlExportServiceImpl implements YmlExportService {

    private final ProductRepository productRepository;

    @Value("${s3.public_url}")
    private String storageUrl;

    @Transactional(readOnly = true)
    public String generateYmlForCandies() {
        // Витягуємо всі товари з категорії CANDIES, які є в наявності
        List<Product> products = productRepository
                .findAllByProductCategory(
                        ProductCategory.CANDIES,
                        Pageable.unpaged() // Якщо Pageable.unpaged() не підтримується, використайте PageRequest.of(0, 10000)
                ).getContent();

        StringBuilder xml = new StringBuilder();

        // Заголовок та кореневий тег
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<yml_catalog date=\"")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("\">\n");

        xml.append("    <shop>\n");
        xml.append("        <name>Moki Market</name>\n");
        xml.append("        <company>Moki</company>\n");
        xml.append("        <url>https://moki.com.ua/</url>\n");

        // Валюти
        xml.append("        <currencies>\n");
        xml.append("            <currency id=\"UAH\" rate=\"1\"/>\n");
        xml.append("            <currency id=\"USD\" rate=\"44.6\"/>\n");
        xml.append("            <currency id=\"EUR\" rate=\"51.2\"/>\n");
        xml.append("        </currencies>\n");

        // Категорії (динамічно або статично, тут залишаємо статично для Цукерок)
        xml.append("        <categories>\n");
        xml.append("            <category id=\"101\">Цукерки</category>\n");
        xml.append("        </categories>\n");

        // Товари
        xml.append("        <offers>\n");

        for (Product product : products) {
            xml.append("            <offer id=\"").append(product.getId()).append("\" available=\"true\">\n");

           BigDecimal currentPrice = product.getPriceWithDiscount() != null ? product.getPriceWithDiscount() : product.getPrice();
           xml.append("                <price>").append(currentPrice).append("</price>\n");
           xml.append("                <price_old>").append(product.getPrice()).append("</price_old>\n");
           xml.append("                <price_promo>").append(product.getPrice()).append("</price_promo>\n");
           xml.append("                <stock_quantity>100</stock_quantity>\n");
           xml.append("                <url>:").append("https://moki.com.ua/products/").append(product.getSlug()).append("</url>\n");
           xml.append("                <currencyId>UAH</currencyId>\n");
           xml.append("                <categoryId>101</categoryId>\n"); // Відповідає category id вище

            for (ProductImage image : product.getImages()) {
                String imageUrl = storageUrl.endsWith("/") ? storageUrl + image.getImageId() : storageUrl + "/" + image.getImageId();
                xml.append("                <picture>").append(imageUrl).append("</picture>\n");
            }

            // Дані про товар[cite: 1]
            xml.append("                <vendor><![CDATA[").append(product.getManufacturerOfTheProduct()).append("]]></vendor>\n");
            xml.append("                <name><![CDATA[").append(product.getName()).append("]]></name>\n");
            xml.append("                <description><![CDATA[").append(product.getDescription()).append("]]></description>\n");

            xml.append("""
                                        <param name="Гарантия" paramid="20769" valueid="11">
                                            <value lang="uk">1 місяць</value>
                                            <value lang="ru">1 месяц</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Вес в упаковке" paramid="48739" valueid="12">
                                            <value lang="uk">1.05</value>
                                            <value lang="ru">1.05</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Срок хранения" paramid="71434" valueid="13">
                                            <value lang="uk">6 місяців</value>
                                            <value lang="ru">6 месяцев</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Состав" paramid="223843" valueid="14">
                                            <value lang="uk">Волоський горіх</value>
                                            <value lang="ru">Грецкий орех</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Вес" paramid="147016" valueid="15">
                                            <value lang="uk">1000</value>
                                            <value lang="ru">1000</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Вид" paramid="" valueid="16">
                                            <value lang="uk">Десерт</value>
                                            <value lang="ru">Десерт</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Упаковка" paramid="147152" valueid="17">
                                            <value lang="uk">Картонна коробка</value>
                                            <value lang="ru">Картонная коробка</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Условия хранения" paramid="137119" valueid="18">
                                            <value lang="uk">У сухому місці</value>
                                            <value lang="ru"> В сухом месте</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Тип шоколада" paramid="" valueid="17">
                                            <value lang="uk">Молочний</value>
                                            <value lang="ru">Молочный</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Страна-производитель товара" paramid="98900" valueid="18">
                                            <value lang="uk">Україна</value>
                                            <value lang="ru">Украина</value>
                                        </param>
                    """);

            xml.append("""
                                        <param name="Количество грузовых мест" paramid="72961" valueid="19">
                                            <value lang="uk">1</value>
                                            <value lang="ru">1</value>
                                        </param>
                    """);

            xml.append("                <param name=\"Підкатегорія\"><![CDATA[").append(product.getSubcategory()).append("]]></param>\n");

            xml.append("            </offer>\n");
        }

        xml.append("        </offers>\n");
        xml.append("    </shop>\n");
        xml.append("</yml_catalog>");

        return xml.toString();
    }
}
