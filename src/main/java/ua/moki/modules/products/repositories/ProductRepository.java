package ua.moki.modules.products.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.enums.ProductCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product>,
        ProductRepositoryCustom {

    Optional<Product> findBySlug(String slug);
    Page<Product> findAll(Pageable pageable);
    Page<Product> findAllByProductCategory(ProductCategory productCategory, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.discount > 0")
    Page<Product> findAllWithDiscount(Pageable pageable);

    Page<Product> findAll(Specification specification, Pageable pageable);

    boolean existsBySlug(String slug);

}
