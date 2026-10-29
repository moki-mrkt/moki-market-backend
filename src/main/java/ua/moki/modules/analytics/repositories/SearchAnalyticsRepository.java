package ua.moki.modules.analytics.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.moki.modules.analytics.domains.SearchLog;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SearchAnalyticsRepository extends JpaRepository<SearchLog, Long>, JpaSpecificationExecutor<SearchLog> {

    long countByCreatedAtGreaterThanEqual(OffsetDateTime since);

    long countByCreatedAtGreaterThanEqualAndNormalizedQueryIsNotNull(OffsetDateTime since);

    long countByCreatedAtGreaterThanEqualAndZeroResultsTrue(OffsetDateTime since);

    @Query("""
            SELECT s.normalizedQuery,
                   COUNT(s),
                   AVG(s.resultsCount),
                   SUM(CASE WHEN s.zeroResults = true THEN 1 ELSE 0 END)
            FROM SearchLog s
            WHERE s.createdAt >= :since
              AND s.normalizedQuery IS NOT NULL
            GROUP BY s.normalizedQuery
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> findTopQueries(@Param("since") OffsetDateTime since, Pageable pageable);

    @Query("""
            SELECT s.normalizedQuery,
                   COUNT(s)
            FROM SearchLog s
            WHERE s.createdAt >= :since
              AND s.normalizedQuery IS NOT NULL
              AND s.zeroResults = true
            GROUP BY s.normalizedQuery
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> findTopZeroResultQueries(@Param("since") OffsetDateTime since, Pageable pageable);

    @Query("""
            SELECT s.category, COUNT(s)
            FROM SearchLog s
            WHERE s.createdAt >= :since
              AND s.category IS NOT NULL
            GROUP BY s.category
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> findTopCategories(@Param("since") OffsetDateTime since);

    @Query(value = """
            SELECT CAST(created_at AT TIME ZONE 'Europe/Kyiv' AS DATE) AS search_day,
                   COUNT(*) AS total_cnt,
                   COALESCE(SUM(CASE WHEN normalized_query IS NOT NULL THEN 1 ELSE 0 END), 0) AS text_cnt,
                   COALESCE(SUM(CASE WHEN zero_results = true THEN 1 ELSE 0 END), 0) AS zero_cnt
            FROM search_analytics
            WHERE created_at >= :since
            GROUP BY search_day
            ORDER BY search_day ASC
            """, nativeQuery = true)
    List<Object[]> findDailyStats(@Param("since") OffsetDateTime since);

    @Modifying
    @Query("DELETE FROM SearchLog s WHERE s.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
