package ua.moki.modules.users.services.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.repositories.RefreshTokenRepository;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        int count = refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Cleaned up {} expired refresh tokens", count);
    }
}