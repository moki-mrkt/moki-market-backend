package ua.moki.modules.analytics.services.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.analytics.domains.SearchLog;
import ua.moki.modules.analytics.dtos.*;
import ua.moki.modules.analytics.events.SearchPerformedEvent;
import ua.moki.modules.analytics.repositories.SearchAnalyticsRepository;
import ua.moki.modules.analytics.services.SearchAnalyticsService;
import ua.moki.modules.products.enums.ProductCategory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAnalyticsServiceImpl implements SearchAnalyticsService {

    private static final long DEDUP_WINDOW_MILLIS = 3 * 60 * 1000L; // 3 хвилини
    private static final int MAX_DEDUP_CACHE_SIZE = 5000;
    private static final Pattern BOT_USER_AGENT_PATTERN = Pattern.compile(
            "(?i).*(bot|crawl|spider|slurp|ahrefs|semrush|lighthouse|headless|facebookexternalhit|telegrambot|bytespider|petalbot).*"
    );

    private final SearchAnalyticsRepository searchAnalyticsRepository;
    private final Clock clock;

    private final Map<String, Long> recentSearchesCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void recordSearchEvent(SearchPerformedEvent event) {
        if (event == null || isBotUserAgent(event.userAgent())) {
            return;
        }

        String rawQuery = sanitizeQuery(event.query());
        String normalizedQuery = normalizeQuery(rawQuery);

        // Якщо немає ні текстового запиту, ні категорії, ні жодного фільтра — пропускаємо
        if (normalizedQuery == null
                && event.category() == null
                && (event.subcategories() == null || event.subcategories().isEmpty())
                && event.minPrice() == null
                && event.maxPrice() == null
                && !Boolean.TRUE.equals(event.hasDiscount())) {
            return;
        }

        String subcategoriesJoined = (event.subcategories() != null && !event.subcategories().isEmpty())
                ? String.join(", ", event.subcategories())
                : null;

        String dedupKey = buildDedupKey(event, normalizedQuery, subcategoriesJoined);
        if (isDuplicateWithinWindow(dedupKey)) {
            return;
        }

        long resultsCount = Math.max(event.resultsCount(), 0L);

        SearchLog searchLog = SearchLog.builder()
                .query(rawQuery)
                .normalizedQuery(normalizedQuery)
                .resultsCount(resultsCount)
                .zeroResults(resultsCount == 0L)
                .category(event.category())
                .subcategories(subcategoriesJoined)
                .minPrice(event.minPrice())
                .maxPrice(event.maxPrice())
                .hasDiscount(event.hasDiscount())
                .userPublicId(event.userPublicId())
                .locale(truncate(event.locale(), 16))
                .clientIp(truncate(event.clientIp(), 64))
                .createdAt(event.occurredAt() != null ? event.occurredAt() : OffsetDateTime.now(clock))
                .build();

        searchAnalyticsRepository.save(searchLog);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchAnalyticsDashboardDTO getDashboardStats(int days) {
        int safeDays = Math.clamp(days, 1, 365);
        OffsetDateTime since = OffsetDateTime.now(clock).minusDays(safeDays);

        long totalSearches = searchAnalyticsRepository.countByCreatedAtGreaterThanEqual(since);
        long textSearches = searchAnalyticsRepository.countByCreatedAtGreaterThanEqualAndNormalizedQueryIsNotNull(since);
        long catalogFilterSearches = Math.max(0L, totalSearches - textSearches);
        long zeroResultSearches = searchAnalyticsRepository.countByCreatedAtGreaterThanEqualAndZeroResultsTrue(since);

        double zeroResultRate = totalSearches > 0
                ? Math.round((zeroResultSearches * 1000.0) / totalSearches) / 10.0
                : 0.0;

        List<TopQueryStatDTO> topQueries = searchAnalyticsRepository
                .findTopQueries(since, PageRequest.of(0, 15))
                .stream()
                .map(row -> new TopQueryStatDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : 0.0,
                        row[3] != null ? ((Number) row[3]).longValue() : 0L
                ))
                .toList();

        List<TopQueryStatDTO> topZeroResultQueries = searchAnalyticsRepository
                .findTopZeroResultQueries(since, PageRequest.of(0, 15))
                .stream()
                .map(row -> {
                    long count = ((Number) row[1]).longValue();
                    return new TopQueryStatDTO((String) row[0], count, 0.0, count);
                })
                .toList();

        List<CategorySearchStatDTO> topCategories = searchAnalyticsRepository
                .findTopCategories(since)
                .stream()
                .map(row -> {
                    ProductCategory cat = (ProductCategory) row[0];
                    long count = ((Number) row[1]).longValue();
                    return new CategorySearchStatDTO(cat, cat != null ? cat.getTitle() : "", count);
                })
                .toList();

        List<DailySearchStatDTO> dailyTrend = searchAnalyticsRepository
                .findDailyStats(since)
                .stream()
                .map(row -> new DailySearchStatDTO(
                        row[0] != null ? row[0].toString() : "",
                        row[1] != null ? ((Number) row[1]).longValue() : 0L,
                        row[2] != null ? ((Number) row[2]).longValue() : 0L,
                        row[3] != null ? ((Number) row[3]).longValue() : 0L
                ))
                .toList();

        return new SearchAnalyticsDashboardDTO(
                totalSearches,
                textSearches,
                catalogFilterSearches,
                zeroResultSearches,
                zeroResultRate,
                topQueries,
                topZeroResultQueries,
                topCategories,
                dailyTrend
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchLogResponseDTO> getSearchLogs(
            String query,
            ProductCategory category,
            Boolean zeroResultsOnly,
            Boolean onlyTextQueries,
            Integer days,
            Pageable pageable
    ) {
        Specification<SearchLog> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (days != null && days > 0) {
                OffsetDateTime since = OffsetDateTime.now(clock).minusDays(days);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), since));
            }

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("normalizedQuery")), pattern));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (Boolean.TRUE.equals(zeroResultsOnly)) {
                predicates.add(cb.isTrue(root.get("zeroResults")));
            }

            if (Boolean.TRUE.equals(onlyTextQueries)) {
                predicates.add(cb.isNotNull(root.get("normalizedQuery")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return searchAnalyticsRepository.findAll(spec, pageable)
                .map(this::toResponseDTO);
    }

    private SearchLogResponseDTO toResponseDTO(SearchLog logEntity) {
        return new SearchLogResponseDTO(
                logEntity.getId(),
                logEntity.getQuery(),
                logEntity.getNormalizedQuery(),
                logEntity.getResultsCount(),
                logEntity.getZeroResults(),
                logEntity.getCategory(),
                logEntity.getCategory() != null ? logEntity.getCategory().getTitle() : null,
                logEntity.getSubcategories(),
                logEntity.getMinPrice(),
                logEntity.getMaxPrice(),
                logEntity.getHasDiscount(),
                logEntity.getUserPublicId(),
                logEntity.getLocale(),
                logEntity.getClientIp(),
                logEntity.getCreatedAt()
        );
    }

    private boolean isBotUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return false;
        }
        return BOT_USER_AGENT_PATTERN.matcher(userAgent).matches();
    }

    private String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return truncate(trimmed, 255);
    }

    private String normalizeQuery(String sanitizedQuery) {
        if (sanitizedQuery == null || sanitizedQuery.isBlank()) {
            return null;
        }
        String collapsed = sanitizedQuery.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return truncate(collapsed, 255);
    }

    private String buildDedupKey(SearchPerformedEvent event, String normalizedQuery, String subcategoriesJoined) {
        String actor = event.userPublicId() != null
                ? event.userPublicId().toString()
                : (event.clientIp() != null ? event.clientIp() : "anon");
        return actor + "|"
                + (normalizedQuery != null ? normalizedQuery : "") + "|"
                + (event.category() != null ? event.category().name() : "") + "|"
                + (subcategoriesJoined != null ? subcategoriesJoined : "") + "|"
                + (event.minPrice() != null ? event.minPrice().stripTrailingZeros().toPlainString() : "") + "|"
                + (event.maxPrice() != null ? event.maxPrice().stripTrailingZeros().toPlainString() : "") + "|"
                + Boolean.TRUE.equals(event.hasDiscount());
    }

    private boolean isDuplicateWithinWindow(String key) {
        long now = clock.millis();
        if (recentSearchesCache.size() > MAX_DEDUP_CACHE_SIZE) {
            recentSearchesCache.entrySet().removeIf(entry -> now - entry.getValue() > DEDUP_WINDOW_MILLIS);
        }
        Long previousTime = recentSearchesCache.put(key, now);
        return previousTime != null && (now - previousTime) < DEDUP_WINDOW_MILLIS;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
