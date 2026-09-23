package ua.moki.modules.products.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.context.i18n.LocaleContextHolder;
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

        String lang = LocaleContextHolder.getLocale().getLanguage();

        Expression<String> subcategoryExpr;
        if ("ru".equalsIgnoreCase(lang)) {
            subcategoryExpr = cb.coalesce(root.get("subcategoryRu"), root.get("subcategory"));
        } else {
            subcategoryExpr = root.get("subcategory");
        }

        query.select(subcategoryExpr).distinct(true);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            query.where(predicate);
        }
        query.orderBy(cb.asc(subcategoryExpr));

        return entityManager.createQuery(query).getResultList();
    }
}
