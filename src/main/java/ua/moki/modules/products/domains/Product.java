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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Column(nullable = false, length = 32)
    String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", nullable = false, length = 32)
    ProductCategory productCategory;
    @Column(nullable = false)
    String description;
    @Column(precision = 19, scale = 2, nullable = false)
    BigDecimal price;
    @Column(precision = 19, scale = 2, nullable = false)
    BigDecimal rating;
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

    public void addImage(ProductImage image) {

        ensureMutableImages();

        this.images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        ensureMutableImages();

        this.images.remove(image);
        image.setProduct(null);
    }

    private void ensureMutableImages() {
        if (this.images == null) {
            this.images = new ArrayList<>();
        } else if (!(this.images instanceof ArrayList)) {

            this.images = new ArrayList<>(this.images);
        }
    }

    public void setImages(List<ProductImage> images) {

        this.images = new ArrayList<>(images);

        this.images.forEach(img -> img.setProduct(this));
    }

}
