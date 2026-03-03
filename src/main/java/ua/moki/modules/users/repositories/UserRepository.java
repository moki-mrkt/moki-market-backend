package ua.moki.modules.users.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.moki.modules.users.domains.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPublicId(UUID publicId);
    Optional<User> findByPublicIdAndDeletedFalse(UUID publicId);

    Page<User> findAll(Pageable pageable);
    Page<User> findAllByDeleted(boolean deleted, Pageable pageable);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.activated = true WHERE u.publicId = :publicId AND u.activated = false")
    int activateUserByPublicId(@Param("publicId") UUID publicId);
}
