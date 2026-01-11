package ua.moki.modules.products.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "image_id", nullable = false)
    private String imageId;
    @Column(name = "is_main")
    private boolean isMain;
    @Column(name = "sort_order")
    private int sortOrder;
    @Column(name = "alt_text")
    private String altText;

    public void updateDetails(boolean isMain, int sortOrder, String altText) {
        this.isMain = isMain;
        this.sortOrder = sortOrder;
        this.altText = altText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductImage that)) return false;
        return id != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
