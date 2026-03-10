package ua.moki.modules.products.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ua.moki.modules.users.domains.User;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "favorite_product", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class FavoriteProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @CreatedDate
    @Column(name = "creation_time", nullable = false, updatable = false)
    private OffsetDateTime creationTime;

    public FavoriteProduct(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}
