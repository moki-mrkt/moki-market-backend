package ua.moki.modules.products.services.exports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductWeightOption;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.enums.ProductType;
import ua.moki.modules.products.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromExportTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("generate outputs <param name=\"Вага\">250 г</param>, <param name=\"Вага\">500 г</param>, <param name=\"Вага\">1 кг</param> and group_id for weight variants")
    void generate_shouldIncludeFormattedWeightParamAndGroupId() {
        PromExport promExport = new PromExport(productRepository, "https://example.com/images/");

        Product product = new Product();
        product.setId(101L);
        product.setName("Кеш'ю смажений");
        product.setNameRu("Кешью жареный");
        product.setSlug("cashew-roasted");
        product.setProductCategory(ProductCategory.NUTS);
        product.setProductType(ProductType.WEIGHT_BASED);
        product.setAvailability(ProductAvailability.IN_STOCK);
        product.setDescription("Опис товару...");

        ProductWeightOption opt250 = new ProductWeightOption();
        opt250.setWeightValue(250);
        opt250.setPrice(new BigDecimal("160"));
        opt250.setIsExported(true);

        ProductWeightOption opt500 = new ProductWeightOption();
        opt500.setWeightValue(500);
        opt500.setPrice(new BigDecimal("310"));
        opt500.setIsExported(true);

        ProductWeightOption opt1000 = new ProductWeightOption();
        opt1000.setWeightValue(1000);
        opt1000.setPrice(new BigDecimal("600"));
        opt1000.setIsExported(true);

        product.setWeightOptions(List.of(opt250, opt500, opt1000));

        when(productRepository.findAllByProductCategory(ProductCategory.NUTS, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(product)));

        String xml = promExport.generate(ProductCategory.NUTS, null);

        assertThat(xml).contains("id=\"101-250\" group_id=\"101\" available=\"true\"");
        assertThat(xml).contains("id=\"101-500\" group_id=\"101\" available=\"true\"");
        assertThat(xml).contains("id=\"101-1000\" group_id=\"101\" available=\"true\"");
        assertThat(xml).contains("<param name=\"Вага\">250 г</param>");
        assertThat(xml).contains("<param name=\"Вага\">500 г</param>");
        assertThat(xml).contains("<param name=\"Вага\">1 кг</param>");
    }
}
