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
public class KastaExport extends AbstractYmlExporter {

    public KastaExport(ProductRepository productRepository, @Value("${s3.public_url}") String storageUrl) {
        super(productRepository, storageUrl);
    }

    @Override
    protected String getMarketplaceName() {
        return "Kasta";
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
        return new BigDecimal("19");
    }

    @Override
    protected List<Param> buildParams(Product p, BigDecimal weightValue, String weightUnit) {
        return List.of(
                new SimpleParam("Колір", "-"),
                new SimpleParam("Розмір", "-"),
                new SimpleParam("Вага, " + weightUnit, String.valueOf(weightValue))
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
                offerName,
                null,
                product.getDescription(),
                null,
                params
        );
    }
}