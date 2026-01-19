package ua.moki.modules.users.security;

import java.time.Instant;
import java.util.List;

public record Token(
        String subject,
        List<String> authorities,
        Instant createdAt,
        Instant expiresAt
) {
}
