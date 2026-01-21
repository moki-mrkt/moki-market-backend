package ua.moki.modules.orders.services.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.orders.repositories.CartRepository;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CartCleanupJob {

    private final CartRepository cartRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteAbandonedCarts() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(2);
        cartRepository.deleteAllByUpdatedAtBefore(threshold);
    }
}
