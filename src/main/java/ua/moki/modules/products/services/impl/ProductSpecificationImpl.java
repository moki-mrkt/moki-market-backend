package ua.moki.modules.products.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.dtos.ProductSearchRequestDTO;
import ua.moki.modules.products.services.ProductSpecifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductSpecificationImpl implements ProductSpecifications {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Specification<Product> getSpecifications(ProductSearchRequestDTO request, boolean excludeSubcategories) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.query() != null && !request.query().isBlank()) {
                String searchTerm = request.query().toLowerCase();

                String searchSchema = "%" + searchTerm + "%";
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchSchema);
                Predicate descLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchSchema);

                Expression<Double> nameSimilarity = criteriaBuilder.function(
                        "similarity",
                        Double.class,
                        criteriaBuilder.lower(root.get("name")),
                        criteriaBuilder.literal(searchTerm)
                );

                Predicate fuzzyMatch = criteriaBuilder.greaterThan(nameSimilarity, 0.3);

                predicates.add(criteriaBuilder.or(nameLike, descLike, fuzzyMatch));
            }

            if (request.category() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productCategory"), request.category()));
            }

            if (request.minPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("priceWithDiscount"), request.minPrice()));
            }

            if (request.maxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("priceWithDiscount"), request.maxPrice()));
            }

            if (!excludeSubcategories && request.subcategory() != null && !request.subcategory().isEmpty()) {
                predicates.add(root.get("subcategory").in(request.subcategory()));
            }

            if (Boolean.TRUE.equals(request.hasDiscount())) {
                predicates.add(criteriaBuilder.greaterThan(root.get("discount"), 0));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public Map<String, Double> getMinMaxPricesBySpecification(Specification<Product> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Product> root = query.from(Product.class);

        Predicate predicate = spec.toPredicate(root, query, cb);

        if (predicate != null) {
            query.where(predicate);
        }

        Expression<Number> pricePath = root.get("priceWithDiscount");

        query.select(
                cb.array(
                        cb.min(pricePath),
                        cb.max(pricePath)
                )
        );

        Object[] result = entityManager.createQuery(query).getSingleResult();

        double min = 0.0;
        double max = 9999.0;

        if (result != null && result.length > 0) {
            if (result[0] != null) min = ((Number) result[0]).doubleValue();
            if (result[1] != null) max = ((Number) result[1]).doubleValue();
        }

        return Map.of(
                "minPrice", Math.floor(min),
                "maxPrice", Math.ceil(max)
        );
    }
}
