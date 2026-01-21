package ua.moki.modules.orders.domains;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import ua.moki.modules.orders.utils.enums.PaymentStatus;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.orders.utils.enums.DeliveryType;
import ua.moki.modules.orders.utils.enums.OrderStatus;
import ua.moki.modules.orders.utils.enums.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "orders")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Version
    Long version;

    @Column(nullable = false, unique = true)
    UUID publicId = UUID.randomUUID();

    @Column(unique = true)
    String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    String email;
    String phoneNumber;
    String firstName;
    String secondName;

    @Enumerated(EnumType.STRING)
    OrderStatus orderStatus = OrderStatus.NEW;
    @Enumerated(EnumType.STRING)
    DeliveryType deliveryType;
    @Enumerated(EnumType.STRING)
    PaymentType paymentType;
    @Enumerated(EnumType.STRING)
    PaymentStatus paymentStatus;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal totalAmount;

    @Embedded
    Address address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreatedDate
    OffsetDateTime createAt;

    @LastModifiedDate
    OffsetDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order that)) return false;
        return id != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
