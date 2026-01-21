package ua.moki.modules.orders.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ua.moki.modules.orders.domains.Cart;
import ua.moki.modules.orders.domains.CartItem;
import ua.moki.modules.orders.dtos.CartItemResponseDTO;
import ua.moki.modules.orders.dtos.CartResponseDTO;
import ua.moki.modules.products.domains.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring", imports = {BigDecimal.class})
public interface CartMapper {

    @Mapping(target = "cartId", source = "id")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalCartPrice", source = "items", qualifiedByName = "calculateCartTotal")
    CartResponseDTO toDto(Cart cart);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "pricePerUnit", source = "product", qualifiedByName = "calculateFinalPricePerUnit")
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "calculateItemTotal")
    CartItemResponseDTO toItemDto(CartItem item);

    List<CartItemResponseDTO> toItemDtoList(List<CartItem> items);

    @Named("calculateFinalPricePerUnit")
    default BigDecimal calculateFinalPricePerUnit(Product product) {
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
    default BigDecimal calculateItemTotal(CartItem item) {
        if (item == null || item.getProduct() == null) return BigDecimal.ZERO;

        BigDecimal finalPrice = calculateFinalPricePerUnit(item.getProduct());
        return finalPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    @Named("calculateCartTotal")
    default BigDecimal calculateCartTotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;

        return items.stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}