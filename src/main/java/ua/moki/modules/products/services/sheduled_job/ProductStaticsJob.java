package ua.moki.modules.products.services.sheduled_job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductStaticsJob {

//    @Scheduled(cron = "0 0 3 * * *")
//    @Transactional
//    public void updateSalesCounts() {
//        // 1. Масовий UPDATE в PostgreSQL (дуже швидко)
//        // Рахуємо кількість проданих товарів за останні 30 днів (тренди) або за весь час
//        String sql = """
//            UPDATE products p
//            SET sales_count = (
//                SELECT COALESCE(SUM(oi.quantity), 0)
//                FROM order_items oi
//                JOIN orders o ON oi.order_id = o.id
//                WHERE oi.product_id = p.id
//                AND o.status = 'COMPLETED'
//            )
//        """;
//        jdbcTemplate.update(sql);
//
//       productService.syncAllProductsToElastic();
//    }
}
