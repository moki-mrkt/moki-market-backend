package ua.moki.modules.users.domains.tokens;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_change_tokens")
public class EmailChangeToken extends BaseToken {

    @Column(name = "new_email", nullable = false)
    private String newEmail;
}
