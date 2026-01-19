package ua.moki.modules.users.domains;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ua.moki.modules.users.utils.enums.RoleType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Version
    Long version;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    UUID publicId = UUID.randomUUID();

    @Column(name = "first_name", nullable = false, length = 64)
    String firstName;
    @Column(name = "second_name", nullable = false, length = 64)
    String secondName;
    @Column(nullable = false, unique = true)
    String email;
    @Column(name = "phone_number", length = 32)
    String phoneNumber;
    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;
    @Column(nullable = false)
    String password;

    String imageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 32)
    RoleType roleType;

    @Column(name = "is_activated")
    boolean activated = false;
    @Column(name = "is_blocked")
    boolean blocked = false;
    @Column(name = "is_deleted")
    boolean deleted = false;

    @Column(name = "access_to_account")
    boolean accessToAccount = true;
    @Column(name = "is_subscribed_to_news")
    boolean subscribedToNews = false;

    @Column(name = "number_of_failed_attempts")
    Integer numberOfFailedAttempts = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    OffsetDateTime creationTime;

    @LastModifiedDate
    @Column(nullable = false)
    OffsetDateTime lastModifiedTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return id != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
