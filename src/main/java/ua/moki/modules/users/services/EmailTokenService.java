package ua.moki.modules.users.services;

import ua.moki.modules.users.dtos.EmailChangeClaimsDTO;

import java.util.UUID;

public interface EmailTokenService {

    String generateEmailChangeToken(UUID userId, String newEmail);

    EmailChangeClaimsDTO parseToken(String tokenString);
}
