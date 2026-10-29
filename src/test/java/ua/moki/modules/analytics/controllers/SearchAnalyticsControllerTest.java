package ua.moki.modules.analytics.controllers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.configuration.entry_points.JwtAuthenticationEntryPoint;
import ua.moki.modules.analytics.dtos.*;
import ua.moki.modules.analytics.services.SearchAnalyticsService;
import ua.moki.modules.products.enums.ProductCategory;
import ua.moki.modules.users.security.JwtFilter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchAnalyticsController.class)
@Import({SecurityConfig.class, JwtCryptoConfig.class, AppConfig.class,
        JwtAuthenticationEntryPoint.class})
class SearchAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchAnalyticsService searchAnalyticsService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("GET /analytics/search/dashboard - 200 OK for ADMIN with dashboard metrics")
    void getDashboard_shouldReturnDashboardStats_whenAdmin() throws Exception {
        SearchAnalyticsDashboardDTO dashboardDTO = new SearchAnalyticsDashboardDTO(
                120L,
                80L,
                40L,
                12L,
                10.0,
                List.of(new TopQueryStatDTO("мигдаль", 25L, 6.5, 1L)),
                List.of(new TopQueryStatDTO("макадамія", 5L, 0.0, 5L)),
                List.of(new CategorySearchStatDTO(ProductCategory.NUTS, "Горіхи", 45L)),
                List.of(new DailySearchStatDTO("2026-10-29", 120L, 80L, 12L))
        );

        when(searchAnalyticsService.getDashboardStats(30)).thenReturn(dashboardDTO);

        mockMvc.perform(get("/analytics/search/dashboard")
                        .with(user("admin").roles("ADMIN"))
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSearches", is(120)))
                .andExpect(jsonPath("$.textSearches", is(80)))
                .andExpect(jsonPath("$.catalogFilterSearches", is(40)))
                .andExpect(jsonPath("$.zeroResultSearches", is(12)))
                .andExpect(jsonPath("$.zeroResultRate", is(10.0)))
                .andExpect(jsonPath("$.topQueries", hasSize(1)))
                .andExpect(jsonPath("$.topQueries[0].query", is("мигдаль")))
                .andExpect(jsonPath("$.topZeroResultQueries", hasSize(1)))
                .andExpect(jsonPath("$.topCategories", hasSize(1)));

        verify(searchAnalyticsService).getDashboardStats(30);
    }

    @Test
    @DisplayName("GET /analytics/search/dashboard - 403 Forbidden for CUSTOMER")
    void getDashboard_shouldReturnForbidden_whenCustomer() throws Exception {
        mockMvc.perform(get("/analytics/search/dashboard")
                        .with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(searchAnalyticsService, never()).getDashboardStats(anyInt());
    }

    @Test
    @DisplayName("GET /analytics/search/dashboard - 401 Unauthorized for anonymous")
    void getDashboard_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        mockMvc.perform(get("/analytics/search/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(searchAnalyticsService, never()).getDashboardStats(anyInt());
    }

    @Test
    @DisplayName("GET /analytics/search/logs - 200 OK for MANAGER with filtered page")
    void getSearchLogs_shouldReturnFilteredPage_whenManager() throws Exception {
        SearchLogResponseDTO logDTO = new SearchLogResponseDTO(
                1L,
                "Мигдаль",
                "мигдаль",
                0L,
                true,
                ProductCategory.NUTS,
                "Горіхи",
                "Мигдаль",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                false,
                UUID.randomUUID(),
                "uk",
                "127.0.0.1",
                OffsetDateTime.now()
        );

        Page<SearchLogResponseDTO> page = new PageImpl<>(List.of(logDTO));

        when(searchAnalyticsService.getSearchLogs(
                eq("мигдаль"),
                eq(ProductCategory.NUTS),
                eq(true),
                eq(true),
                eq(7),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/analytics/search/logs")
                        .with(user("manager").roles("MANAGER"))
                        .param("query", "мигдаль")
                        .param("category", "NUTS")
                        .param("zeroResultsOnly", "true")
                        .param("onlyTextQueries", "true")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].normalizedQuery", is("мигдаль")))
                .andExpect(jsonPath("$.content[0].zeroResults", is(true)));

        verify(searchAnalyticsService).getSearchLogs(
                eq("мигдаль"),
                eq(ProductCategory.NUTS),
                eq(true),
                eq(true),
                eq(7),
                any(Pageable.class)
        );
    }
}
