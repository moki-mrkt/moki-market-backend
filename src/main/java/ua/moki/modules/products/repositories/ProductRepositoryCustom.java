package ua.moki.modules.products.repositories;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import ua.moki.modules.products.domains.Product;

import java.util.List;

@Repository
public interface ProductRepositoryCustom {

    List<String> findDistinctSubcategoriesBySpec(Specification<Product> spec);
}
