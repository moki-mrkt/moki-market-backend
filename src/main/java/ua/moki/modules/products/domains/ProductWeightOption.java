package ua.moki.modules.products.domains;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_weight_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductWeightOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "weight_value", nullable = false)
    private Integer weightValue; // Вага в грамах

    @Column(nullable = false)
    private BigDecimal price; // Фіксована ціна за цю упаковку

    @Column(name = "is_default")
    private Boolean isDefault = false;
}
