package ua.moki.modules.orders.utils.mappers;

import org.mapstruct.*;
import ua.moki.modules.orders.domains.Address;
import ua.moki.modules.orders.domains.Order;
import ua.moki.modules.orders.domains.OrderItem;
import ua.moki.modules.orders.dtos.*;
import ua.moki.modules.products.domains.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {BigDecimal.class})
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "orderStatus", constant = "NEW")
    @Mapping(target = "paymentStatus", constant = "PENDING")
    @Mapping(source = "addressDTO", target = "address")
    Order toEntity(OrderRequestDTO dto);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "addressDTO", source = "address")
    @Mapping(target = "total", source = "totalAmount")
    @Mapping(target = "discountTotal", source = ".", qualifiedByName = "calculateDiscount")
    @Mapping(target = "itemsTotal", source = ".", qualifiedByName = "calculateItemsTotal")
    OrderResponseDTO toDto(Order order);


    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "itemName", source = "product.name")
    @Mapping(target = "totalAmount", expression = "java(item.getFinalPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())))")
    OrderItemDTO toItemDTO(OrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantity", source = "dto.quantity")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "order", source = "order")
    @Mapping(target = "basePriceAtPurchase", source = "product.price")
    @Mapping(target = "discountPercentageAtPurchase", source = "product.discount")
    @Mapping(target = "discountAmountPerUnit", ignore = true)
    @Mapping(target = "finalPricePerUnit", ignore = true)
    OrderItem toOrderItem(CartItemDTO dto, Product product, Order order);

    AddressDTO toAddressDTO(Address address);

    Address toAddress(AddressDTO dto);

    @Named("calculateDiscount")
    default BigDecimal calculateDiscount(Order order) {
        if (order.getItems() == null) return BigDecimal.ZERO;

        return order.getItems().stream()
                .map(item -> item.getDiscountAmountPerUnit()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Named("calculateItemsTotal")
    default BigDecimal calculateItemsTotal(Order order) {
        if (order.getItems() == null) return BigDecimal.ZERO;

        return order.getItems().stream()
                .map(item -> item.getBasePriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @AfterMapping
    default void calculatePrices(@MappingTarget OrderItem item, Product product) {

        int discountPercent = (product.getDiscount() != null) ? product.getDiscount() : 0;

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountPercent > 0) {
            discountAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(discountPercent))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }

        item.setDiscountAmountPerUnit(discountAmount);
        item.setFinalPricePerUnit(product.getPrice().subtract(discountAmount));
    }
}