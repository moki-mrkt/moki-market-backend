package ua.moki.modules.users.services.impl;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.services.UserSpecifications;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserSpecificationImpl implements UserSpecifications {

    @Override
    public Specification<User> getSpecifications(String query, Boolean isDeleted) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isDeleted != null) {
                predicates.add(cb.equal(root.get("deleted"), isDeleted));
            }

            if (query != null && !query.isBlank()) {
                String searchTerm = query.toLowerCase();
                String searchSchema = "%" + searchTerm + "%";

                Predicate emailLike = cb.like(cb.lower(root.get("email")), searchSchema);

                Predicate publicIdLike = cb.like(
                        cb.function("text", String.class, root.get("publicId")),
                        searchSchema
                );

                Expression<Double> nameSimilarity = cb.function(
                        "similarity",
                        Double.class,
                        cb.concat(cb.concat(root.get("firstName"), " "), root.get("secondName")),
                        cb.literal(searchTerm)
                );
                Predicate fuzzyName = cb.greaterThan(nameSimilarity, 0.3);

                predicates.add(cb.or(emailLike, publicIdLike, fuzzyName));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
