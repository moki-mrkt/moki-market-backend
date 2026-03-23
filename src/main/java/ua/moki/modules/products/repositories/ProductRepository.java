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
import ua.moki.modules.products.enums.ProductAvailability;
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
    Page<Product> findAllByAvailability(Pageable pageable, ProductAvailability availability);
    Page<Product> findAllByProductCategoryAndAvailability(ProductCategory productCategory, ProductAvailability availability, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.discount > 0 AND p.availability = 'IN_STOCK' ")
    Page<Product> findAllWithDiscount(Pageable pageable);

    Page<Product> findAll(Specification specification, Pageable pageable);

    boolean existsBySlug(String slug);

}
