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

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {BigDecimal.class})
public abstract class CartMapper {

    @Value("${s3.public_url}")
    protected String storageUrl;

    @Mapping(target = "cartId", source = "id")
    @Mapping(target = "items", source = "items", qualifiedByName = "sortCartItems") // <--- Обов'язково тут!
    @Mapping(target = "totalCartPrice", source = "items", qualifiedByName = "calculateCartTotal")
    public abstract CartResponseDTO toDto(Cart cart);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", source = "product.price")
    @Mapping(target = "productImage", source = "product", qualifiedByName = "getMainImageId") // source змінено на "product"
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "currentPrice", source = "product", qualifiedByName = "calculateFinalPricePerUnit")
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

        return storageUrl.endsWith("/") ? storageUrl + imageId : storageUrl + "/" + imageId;
    }

    @Named("calculateFinalPricePerUnit")
    protected BigDecimal calculateFinalPricePerUnit(Product product) {
        if (product == null) return BigDecimal.ZERO;

        BigDecimal price = product.getPrice();
        int discount = (product.getDiscount() != null) ? product.getDiscount() : 0;

        if (discount == 0) return price;

        BigDecimal discountAmount = price
                .multiply(BigDecimal.valueOf(discount))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        return price.subtract(discountAmount);
    }

    @Named("calculateItemTotal")
    protected BigDecimal calculateItemTotal(CartItem item) {
        if (item == null || item.getProduct() == null) return BigDecimal.ZERO;

        BigDecimal finalPrice = calculateFinalPricePerUnit(item.getProduct());
        return finalPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    @Named("calculateCartTotal")
    protected BigDecimal calculateCartTotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;

        return items.stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}