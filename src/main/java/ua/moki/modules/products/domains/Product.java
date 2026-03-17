package ua.moki.modules.products.domains;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "products")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 64)
    String name;
    @Column(name = "slug", unique = true, nullable = false)
    String slug;
    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", nullable = false, length = 32)
    ProductCategory productCategory;
    @Column(nullable = false)
    String description;
    @Column(precision = 19, scale = 2, nullable = false)
    BigDecimal price;
    @Column(name = "price_with_discount", precision = 19, scale = 2)
    private BigDecimal priceWithDiscount;
    @Column(precision = 19, scale = 2, nullable = false)
    BigDecimal rating;
    @Column(name = "reviews_count", nullable = false)
    private Long reviewsCount = 0L;
    @Enumerated(EnumType.STRING)
    @Column( nullable = false, length = 32)
    ProductAvailability availability;
    @Column(nullable = false)
    Integer discount;
    @Column(precision = 19, scale = 2, nullable = false)
    BigDecimal purchasePrice;
    @Column(nullable = false, length = 32)
    String manufacturerOfTheProduct;
    @Column(nullable = false)
    String subcategory;
    @Column(name = "sales_count")
    Long salesCount = 0L;
    @Column(nullable = false)
    String initOfMeasure;
    @Column(nullable = false)
    Integer valueOfInitOfMeasure;
    @CreatedDate
    @Column(name = "creation_time", nullable = false, updatable = false)
    OffsetDateTime creationTime;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    Map<String, String> characteristics = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product that)) return false;
        return id != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


    public void syncImages(List<ProductImage> incomingImages) {
        if (incomingImages == null) {
            this.images.clear();
            return;
        }

        Map<Long, ProductImage> newImagesMap = incomingImages.stream()
                .filter(img -> img.getId() != null)
                .collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        Iterator<ProductImage> iterator = this.images.iterator();
        while (iterator.hasNext()) {
            ProductImage existingImage = iterator.next();

            if (existingImage.getId() != null && !newImagesMap.containsKey(existingImage.getId())) {
                iterator.remove();
                existingImage.setProduct(null);
            }
        }

        for (ProductImage newImage : incomingImages) {
            if (newImage.getId() == null) {
                newImage.setProduct(this);
                this.images.add(newImage);
            } else {
                this.images.stream()
                        .filter(img -> img.getId().equals(newImage.getId()))
                        .findFirst()
                        .ifPresent(existing -> {
                        });
            }
        }
    }

    @PrePersist
    @PreUpdate
    public void updatePriceWithDiscount() {
        if (this.price == null) {
            return;
        }

        if (this.discount == null || this.discount == 0) {
            this.priceWithDiscount = this.price;
        } else {
            BigDecimal discountPercentage = BigDecimal.valueOf(this.discount);
            BigDecimal discountAmount = this.price
                    .multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            this.priceWithDiscount = this.price.subtract(discountAmount);
        }
    }
}
