package ua.moki.modules.users.dtos;

import java.util.UUID;

public record EmailChangeClaimsDTO(
        UUID userId, String newEmail
) {
}
