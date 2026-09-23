package ua.moki.modules.orders.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import ua.moki.modules.orders.domains.Cart;
import ua.moki.modules.orders.domains.CartItem;
import ua.moki.modules.orders.dtos.CartItemResponseDTO;
import ua.moki.modules.orders.dtos.CartResponseDTO;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.domains.ProductWeightOption;
import ua.moki.modules.products.enums.ProductType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {BigDecimal.class})
public abstract class CartMapper {

    @Value("${s3.public_url}")
    protected String storageUrl;

    @Mapping(target = "cartId", source = "id")
    @Mapping(target = "items", source = "items", qualifiedByName = "sortCartItems")
    @Mapping(target = "totalCartPrice", source = "items", qualifiedByName = "calculateCartTotal")
    public abstract CartResponseDTO toDto(Cart cart);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", expression = "java(getLocalizedName(item.getProduct()))")
    @Mapping(target = "productPrice", source = ".", qualifiedByName = "calculateBasePricePerUnit")
    @Mapping(target = "productImage", source = "product", qualifiedByName = "getMainImageId")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "currentPrice", source = ".", qualifiedByName = "calculateFinalPricePerUnit")
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "calculateItemTotal")
    public abstract CartItemResponseDTO toItemDto(CartItem item);

    public abstract List<CartItemResponseDTO> toItemDtoList(List<CartItem> items);

    @Named("sortCartItems")
    protected List<CartItemResponseDTO> sortCartItems(List<CartItem> items) {
        if (items == null || items.isEmpty()) return new ArrayList<>();

        return items.stream()
                .sorted(Comparator.comparing(CartItem::getCreateAt, Comparator.nullsFirst(Comparator.reverseOrder())))
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    @Named("getMainImageId")
    protected String getMainImageId(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }

        String imageId = product.getImages().stream()
                .filter(ProductImage::isMain)
                .findFirst()
                .orElse(product.getImages().getFirst())
                .getImageId();

        return storageUrl + imageId;
    }

    @Named("calculateBasePricePerUnit")
    protected BigDecimal calculateBasePricePerUnit(CartItem item) {
        if (item == null || item.getProduct() == null) return BigDecimal.ZERO;

        Product p = item.getProduct();

        if (p.getProductType() == ProductType.WEIGHT_BASED && item.getWeight() != null) {
            Optional<ProductWeightOption> fixedOption = p.getWeightOptions().stream()
                    .filter(opt -> opt.getWeightValue().equals(item.getWeight()))
                    .findFirst();

            if (fixedOption.isPresent()) {
                return fixedOption.get().getPrice();
            } else {
                BigDecimal weightMultiplier = BigDecimal.valueOf(item.getWeight())
                        .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
                BigDecimal customPrice = p.getPrice().multiply(weightMultiplier);
                return customPrice.setScale(2, RoundingMode.HALF_UP);
            }
        }

        return p.getPrice();
    }

    @Named("calculateFinalPricePerUnit")
    protected BigDecimal calculateFinalPricePerUnit(CartItem item) {
        if (item == null || item.getProduct() == null) return BigDecimal.ZERO;

        Product p = item.getProduct();

        BigDecimal basePrice = calculateBasePricePerUnit(item);

        int discount = (p.getDiscount() != null) ? p.getDiscount() : 0;

        if (discount == 0) return basePrice;

        BigDecimal discountAmount = basePrice
                .multiply(BigDecimal.valueOf(discount))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        return basePrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }

    @Named("calculateItemTotal")
    protected BigDecimal calculateItemTotal(CartItem item) {
        if (item == null) return BigDecimal.ZERO;

        BigDecimal finalPrice = calculateFinalPricePerUnit(item);
        return finalPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
    }

    @Named("calculateCartTotal")
    protected BigDecimal calculateCartTotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;

        return items.stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Named("getLocalizedName")
    protected String getLocalizedName(Product product) {
        if (product == null) return null;

        String lang = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage();

        if ("ru".equalsIgnoreCase(lang) && product.getNameRu() != null && !product.getNameRu().isBlank()) {
            return product.getNameRu();
        }

        return product.getName();
    }
}