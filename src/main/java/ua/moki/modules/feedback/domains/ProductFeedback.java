package ua.moki.modules.feedback.domains;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ua.moki.modules.products.domains.Product;

@Entity
@Getter
@Setter
@NoArgsConstructor
@DiscriminatorValue("PRODUCT")
public class ProductFeedback extends Feedback{
    @ManyToOne
    private Product product;
}
