package ua.moki.modules.products.services.exports;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.yml_catalog.Offer;
import ua.moki.modules.products.dtos.yml_catalog.Param;
import ua.moki.modules.products.dtos.yml_catalog.SimpleParam;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PromExport extends AbstractYmlExporter {

    public PromExport(ProductRepository productRepository,
                      @Value("${s3.public_url}") String storageUrl) {
        super(productRepository, storageUrl);
    }

    @Override
    protected String getMarketplaceName() {
        return "Prom";
    }

    @Override
    protected String getMarketplaceCategoryId(ProductCategory category) {
        return category.getPromId();
    }

    @Override
    protected BigDecimal getCommissionRate() {
        return new BigDecimal("0.072");
    }

    @Override
    protected BigDecimal getFixedFee(BigDecimal estimatedFinalPrice) {
        if (estimatedFinalPrice.compareTo(new BigDecimal("700")) >= 0) {
            return new BigDecimal("30");
        }
        return new BigDecimal("10");
    }

    @Override
    protected List<Param> buildParams(Product product, BigDecimal weightValue, String weightUnit) {
        return List.of(
                new SimpleParam("Гарантія", "1 місяць"),
                new SimpleParam("Вага", weightUnit, String.valueOf(weightValue))
        );
    }

    @Override
    protected Offer createOffer(Product product, String offerId, String available,
                                BigDecimal calculatedPrice, String offerName, String offerNameRu,
                                String htmlDescription, String htmlDescriptionRu, List<Param> params) {
        return new Offer(
                offerId,
                available,
                "true",
                "r",
                calculatedPrice,
                null,
                null,
                offerId + "-ART",
                null,
                null,
                "UAH",
                "101",
                getPictureUrls(product, 10),
                product.getManufacturerOfTheProduct(),
                offerNameRu,
                offerName,
                htmlDescriptionRu,
                htmlDescription,
                params
        );
    }
}