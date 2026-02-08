package ua.moki.modules.products.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.repositories.ProductRepositoryCustom;

import java.util.List;

public class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<String> findDistinctSubcategoriesBySpec(Specification<Product> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<Product> root = query.from(Product.class);

        query.select(root.get("subcategory")).distinct(true);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            query.where(predicate);
        }

        query.orderBy(cb.asc(root.get("subcategory")));

        return entityManager.createQuery(query).getResultList();
    }
}
