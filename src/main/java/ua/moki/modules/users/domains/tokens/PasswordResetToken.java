package ua.moki.modules.users.domains.tokens;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseToken {
}
