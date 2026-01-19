package ua.moki.modules.orders.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ua.moki.modules.products.domains.Product;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basePriceAtPurchase;

    @Column(nullable = false)
    private Integer discountPercentageAtPurchase;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmountPerUnit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPricePerUnit;
}
