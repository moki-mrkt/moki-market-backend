package ua.moki.modules.products.services.exports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import lombok.RequiredArgsConstructor;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.yml_catalog.*;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.products.enums.ProductType;
import ua.moki.modules.products.repositories.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class AbstractYmlExporter {

    protected final ProductRepository productRepository;
    protected final String storageUrl;
    protected final XmlMapper xmlMapper;

    protected final Parser markdownParser = Parser.builder().build();
    protected final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    public AbstractYmlExporter(ProductRepository productRepository, String storageUrl) {
        this.productRepository = productRepository;
        this.storageUrl = storageUrl;
        this.xmlMapper = XmlMapper.builder()
                .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
                .build();
    }

    @Transactional(readOnly = true)
    public String generate(ProductCategory category, String subcategory) {

        List<Offer> offers = getOffersStream(category, subcategory, this::mapToOffer).toList();

        List<Currency> currencies = List.of(
                new Currency("UAH", "1"),
                new Currency("USD", "44.6"),
                new Currency("EUR", "51.2")
        );

        Shop shop = new Shop(
                "Moki Market",
                "Moki",
                "https://moki.com.ua/",
                currencies,
                getCategories(category),
                offers
        );

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        YmlCatalog catalog = new YmlCatalog(date, shop);

        try {
            return xmlMapper.writeValueAsString(catalog);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Помилка генерації YML для " + getMarketplaceName(), e);
        }
    }

    protected abstract String getMarketplaceName();
    protected abstract String getMarketplaceCategoryId(ProductCategory category);
    protected abstract BigDecimal getCommissionRate();
    protected abstract BigDecimal getFixedFee(BigDecimal estimatedFinalPrice);

    // Специфічні деталі конструювання Offer під конкретний маркетплейс
    protected abstract List<Param> buildParams(Product product, BigDecimal weightValue, String weightUnit);
    protected abstract Offer createOffer(Product product, String offerId, String available,
                                         BigDecimal calculatedPrice, String offerName, String offerNameRu,
                                         String htmlDescription, String htmlDescriptionRu, List<Param> params);

    @FunctionalInterface
    public interface OfferMapper {
        Offer map(Product product, String offerId, String offerName,
                  BigDecimal currentPrice, BigDecimal currentPurchasePrice,
                  BigDecimal weightValue, String weightUnit);
    }

    protected Offer mapToOffer(Product product,
                               String offerId,
                               String offerName,
                               BigDecimal targetPrice,
                               BigDecimal targetPurchasePrice,
                               BigDecimal weightValue,
                               String weightUnit) {
        BigDecimal calculatedBasePrice = calculatePrice(targetPrice, targetPurchasePrice);
        String available = product.getAvailability() == ProductAvailability.IN_STOCK ? "true" : "false";

        String offerNameRu = offerName;
        if (product.getNameRu() != null && !product.getNameRu().isBlank()) {
            offerNameRu = offerName.replace(product.getName(), product.getNameRu());
        }

        String htmlDescription = product.getDescription() != null
                ? htmlRenderer.render(markdownParser.parse(product.getDescription()))
                : "";

        String htmlDescriptionRu = (product.getDescriptionRu() == null || product.getDescriptionRu().isBlank())
                ? htmlDescription
                : htmlRenderer.render(markdownParser.parse(product.getDescriptionRu()));

        List<Param> params = buildParams(product, weightValue, weightUnit);

        return createOffer(product, offerId, available, calculatedBasePrice,
                offerName, offerNameRu, htmlDescription, htmlDescriptionRu, params);
    }

    protected List<Category> getCategories(ProductCategory category) {
        String internalId = String.valueOf(category.ordinal() + 1);
        String marketplaceId = getMarketplaceCategoryId(category);
        return List.of(new Category(internalId, null, marketplaceId, category.getTitle()));
    }

    protected Stream<Offer> getOffersStream(ProductCategory category, String subcategory, OfferMapper mapper) {
        List<Product> products = (subcategory != null && !subcategory.isBlank())
                ? productRepository.findAllByProductCategoryAndSubcategory(category, subcategory, Pageable.unpaged()).getContent()
                : productRepository.findAllByProductCategory(category, Pageable.unpaged()).getContent();

        return products.stream().flatMap(product -> {
            if (product.getProductType() == ProductType.WEIGHT_BASED
                    && product.getWeightOptions() != null
                    && !product.getWeightOptions().isEmpty()) {

                return product.getWeightOptions().stream()
                        .filter(opt -> Boolean.TRUE.equals(opt.getIsExported()))
                        .map(opt -> {
                            String uniqueId = product.getId() + "-" + opt.getWeightValue();
                            String weightName = product.getName() + " " + opt.getWeightValue() + "г";

                            BigDecimal optionPurchasePrice = null;
                            if (product.getPurchasePrice() != null) {
                                BigDecimal ratio = BigDecimal.valueOf(opt.getWeightValue())
                                        .divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
                                optionPurchasePrice = product.getPurchasePrice().multiply(ratio);
                            }

                            return mapper.map(
                                    product,
                                    uniqueId,
                                    weightName,
                                    opt.getPrice(),
                                    optionPurchasePrice,
                                    BigDecimal.valueOf(opt.getWeightValue()),
                                    "г"
                            );
                        });
            }

            return Stream.of(mapper.map(
                    product,
                    String.valueOf(product.getId()),
                    product.getName(),
                    product.getPrice(),
                    product.getPurchasePrice(),
                    BigDecimal.valueOf(product.getValueOfInitOfMeasure()),
                    product.getInitOfMeasure()
            ));
        });
    }

    protected List<String> getPictureUrls(Product product, Integer limit) {
        var stream = product.getImages().stream();
        if (limit != null) {
            stream = stream.limit(limit);
        }
        return stream.map(image -> storageUrl + image.getImageId() + "_large.webp").toList();
    }

    protected BigDecimal calculatePrice(BigDecimal price, BigDecimal purchasePrice) {
        BigDecimal commissionRate = getCommissionRate();
        BigDecimal divisor = BigDecimal.ONE.subtract(commissionRate);

        boolean hasPurchasePrice = purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal targetProfit = BigDecimal.ZERO;

        BigDecimal estimatedPrice = price.add(BigDecimal.TEN).divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal fixedFee = getFixedFee(estimatedPrice);
        BigDecimal exactFinalPrice = price.add(fixedFee).divide(divisor, 2, RoundingMode.HALF_UP);

        long currentVal = exactFinalPrice.setScale(0, RoundingMode.HALF_UP).longValue();
        BigDecimal roundedPriceObj = BigDecimal.valueOf(currentVal);
        BigDecimal marketplaceCommission = roundedPriceObj.multiply(commissionRate);
        BigDecimal revenueAfterFees = roundedPriceObj.subtract(marketplaceCommission).subtract(fixedFee);

        boolean roundUp;
        if (hasPurchasePrice) {
            BigDecimal profit = revenueAfterFees.subtract(purchasePrice);
            roundUp = profit.compareTo(targetProfit) < 0;
        } else {
            roundUp = revenueAfterFees.compareTo(price) < 0;
        }

        long remainder = currentVal % 10;
        long finalPriceValue;

        if (roundUp) {
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
