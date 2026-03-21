package ua.moki.modules.users.services;

import org.springframework.data.jpa.domain.Specification;
import ua.moki.modules.users.domains.User;

public interface UserSpecifications {
    Specification<User> getSpecifications(String query, Boolean isDeleted);
}
