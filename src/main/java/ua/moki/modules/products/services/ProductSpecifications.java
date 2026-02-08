package ua.moki.modules.products.services;

import org.springframework.data.jpa.domain.Specification;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.ProductSearchRequestDTO;

import java.util.Map;

public interface ProductSpecifications {
    Specification<Product> getSpecifications(ProductSearchRequestDTO request, boolean excludeSubcategories);
    Map<String, Double> getMinMaxPricesBySpecification(Specification<Product> spec);
}
