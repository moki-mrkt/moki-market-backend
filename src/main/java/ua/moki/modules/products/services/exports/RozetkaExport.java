package ua.moki.modules.products.services.exports;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.yml_catalog.*;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RozetkaExport extends AbstractYmlExporter {

    public RozetkaExport(ProductRepository productRepository,
                         @Value("${s3.public_url}") String storageUrl) {
        super(productRepository, storageUrl);
    }

    @Override
    protected String getMarketplaceName() {
        return "Rozetka";
    }

    @Override
    protected String getMarketplaceCategoryId(ProductCategory category) {
        return category.getRozetkaId();
    }

    @Override
    protected BigDecimal getCommissionRate() {
        return new BigDecimal("0.16");
    }

    @Override
    protected BigDecimal getFixedFee(BigDecimal estimatedFinalPrice) {
        return new BigDecimal("12");
    }

    @Override
    protected List<Param> buildParams(Product p, BigDecimal weightValue, String weightUnit) {
        String weightStr = String.valueOf(weightValue);
        return List.of(
                new ComplexParam("Гарантія", "20769", "11", List.of(
                        new ParamValue("uk", "1 місяць"), new ParamValue("ru", "1 месяц")
                )),
                new ComplexParam("Вага в упаковці, " + weightUnit, "48739", "12", List.of(
                        new ParamValue("uk", weightStr),
                        new ParamValue("ru", weightStr)
                )),
                new ComplexParam("Вес, " + weightUnit, "147016", "15", List.of(
                        new ParamValue("uk", weightStr),
                        new ParamValue("ru", weightStr)
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
                new ComplexParam("Кількість вантажних місць", "72961", "21", List.of(
                        new ParamValue("uk", "1"),
                        new ParamValue("ru", "1")
                )),
                new SimpleParam("Підкатегорія", p.getSubcategory())
        );
    }

    @Override
    protected Offer createOffer(Product product, String offerId, String available,
                                BigDecimal calculatedPrice, String offerName, String offerNameRu,
                                String htmlDescription, String htmlDescriptionRu, List<Param> params) {
        return new Offer(
                offerId,
                available,
                null,
                null,
                calculatedPrice,
                calculatedPrice.add(BigDecimal.TEN),
                calculatedPrice.subtract(BigDecimal.TEN),
                offerId + "-ART",
                100,
                "https://moki.com.ua/products/" + product.getSlug(),
                "UAH",
                "101",
                getPictureUrls(product, null),
                product.getManufacturerOfTheProduct(),
                offerNameRu,
                offerName,
                htmlDescriptionRu,
                htmlDescription,
                params
        );
    }
}