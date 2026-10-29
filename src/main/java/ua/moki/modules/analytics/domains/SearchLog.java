package ua.moki.modules.analytics.domains;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "search_analytics")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "query", length = 255)
    String query;

    @Column(name = "normalized_query", length = 255)
    String normalizedQuery;

    @Column(name = "results_count", nullable = false)
    Long resultsCount;

    @Column(name = "zero_results", nullable = false)
    Boolean zeroResults;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    ProductCategory category;

    @Column(name = "subcategories")
    String subcategories;

    @Column(name = "min_price", precision = 19, scale = 2)
    BigDecimal minPrice;

    @Column(name = "max_price", precision = 19, scale = 2)
    BigDecimal maxPrice;

    @Column(name = "has_discount")
    Boolean hasDiscount;

    @Column(name = "user_public_id")
    UUID userPublicId;

    @Column(name = "locale", length = 16)
    String locale;

    @Column(name = "client_ip", length = 64)
    String clientIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
