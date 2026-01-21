package ua.moki.modules.feedbacks.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.configuration.entry_points.JwtAuthenticationEntryPoint;
import ua.moki.modules.feedback.controllers.FeedbackController;
import ua.moki.modules.feedback.dtos.FeedbackAnswerDTO;
import ua.moki.modules.feedback.dtos.FeedbackRequestDTO;
import ua.moki.modules.feedback.dtos.FeedbackResponseDTO;
import ua.moki.modules.feedback.dtos.FeedbackUpdateDTO;
import ua.moki.modules.feedback.services.FeedbackService;
import ua.moki.modules.users.security.JwtFilter;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringDeserializer;
import ua.moki.util.exceptions.EntityNotFoundException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@Import({SecurityConfig.class, JwtCryptoConfig.class, AppConfig.class, JwtAuthenticationEntryPoint.class})
public class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private AccessTokenJwsStringDeserializer accessTokenDeserializer;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() throws ServletException, IOException {

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST /feedbacks should return 200 OK and created feedback")
    void create_shouldReturnOk() throws Exception {

        UUID userId = UUID.randomUUID();
        FeedbackRequestDTO requestDTO = new FeedbackRequestDTO(1L,"Great product", 5);
        FeedbackResponseDTO responseDTO = new FeedbackResponseDTO(
                1L, "Great product", 5, "Ivan", "url", OffsetDateTime.now(), null, null
        );

        when(feedbackService.createFeedback(eq(userId), any(FeedbackRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/feedbacks")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.comment").value("Great product"))
                .andExpect(jsonPath("$.firstNameUser").value("Ivan"));

        verify(feedbackService).createFeedback(eq(userId), any(FeedbackRequestDTO.class));
    }

    @Test
    @DisplayName("POST /feedbacks should return 401 Unauthorized for anonymous user")
    void create_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        FeedbackRequestDTO requestDTO = new FeedbackRequestDTO(null, "Comment", 5);

        mockMvc.perform(post("/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("POST /feedbacks should return 400 BadRequest when validation fails")
    void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
        FeedbackRequestDTO invalidDTO = new FeedbackRequestDTO(null, "", 6);

        mockMvc.perform(post("/feedbacks")
                        .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id} should return 200 OK and updated feedback for owner")
    void updateFeedback_shouldReturnOk() throws Exception {

        Long feedbackId = 1L;
        UUID userId = UUID.randomUUID();
        FeedbackUpdateDTO updateDTO = new FeedbackUpdateDTO("Updated review comment", 4);
        FeedbackResponseDTO responseDTO = new FeedbackResponseDTO(
                feedbackId, "Updated review comment", 4, "Ivan", "url", OffsetDateTime.now(), null, null
        );

        when(feedbackService.updateFeedback(eq(feedbackId), any(FeedbackUpdateDTO.class), any(Authentication.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/feedbacks/{id}", feedbackId)
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedbackId))
                .andExpect(jsonPath("$.comment").value("Updated review comment"))
                .andExpect(jsonPath("$.rating").value(4));

        verify(feedbackService).updateFeedback(eq(feedbackId), any(FeedbackUpdateDTO.class), any(Authentication.class));
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id} should return 401 Unauthorized for anonymous user")
    void updateFeedback_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        Long feedbackId = 1L;
        FeedbackUpdateDTO updateDTO = new FeedbackUpdateDTO("Comment", 5);

        mockMvc.perform(patch("/feedbacks/{id}", feedbackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id} should return 400 BadRequest when validation fails")
    void updateFeedback_shouldReturnBadRequest_whenInvalidData() throws Exception {
        Long feedbackId = 1L;
        FeedbackUpdateDTO invalidDTO = new FeedbackUpdateDTO("", 10);

        mockMvc.perform(patch("/feedbacks/{id}", feedbackId)
                        .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id} should return 404 Not Found when feedback does not exist")
    void updateFeedback_shouldReturnNotFound_whenFeedbackDoesNotExist() throws Exception {
        Long feedbackId = 999L;
        FeedbackUpdateDTO updateDTO = new FeedbackUpdateDTO("Comment", 5);

        when(feedbackService.updateFeedback(eq(feedbackId), any(FeedbackUpdateDTO.class), any(Authentication.class)))
                .thenThrow(new EntityNotFoundException("Feedback not found"));

        mockMvc.perform(patch("/feedbacks/{id}", feedbackId)
                        .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id}/answer should return 200 OK when Admin adds an answer")
    void addAnswer_shouldReturnOk_whenUserIsAdmin() throws Exception {

        Long feedbackId = 1L;
        FeedbackAnswerDTO answerDTO = new FeedbackAnswerDTO("Дякуємо за ваш відгук!");
        FeedbackResponseDTO responseDTO = new FeedbackResponseDTO(
                feedbackId, "Коментар користувача", 5, "Ivan", "url",
                OffsetDateTime.now(), "Дякуємо за ваш відгук!", OffsetDateTime.now()
        );

        when(feedbackService.addAnswerToFeedback(eq(feedbackId), eq("ROLE_ADMIN"), any(FeedbackAnswerDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/feedbacks/{id}/answer", feedbackId)
                        .with(user("admin").roles("ADMIN")) // Spring Security автоматично додасть ROLE_
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Дякуємо за ваш відгук!"))
                .andExpect(jsonPath("$.answeredAt").exists());

        verify(feedbackService).addAnswerToFeedback(eq(feedbackId), eq("ROLE_ADMIN"), any(FeedbackAnswerDTO.class));
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id}/answer should return 400 BadRequest when answer is blank")
    void addAnswer_shouldReturnBadRequest_whenAnswerIsInvalid() throws Exception {
        Long feedbackId = 1L;
        FeedbackAnswerDTO invalidDTO = new FeedbackAnswerDTO("");

        mockMvc.perform(patch("/feedbacks/{id}/answer", feedbackId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id}/answer should return 401 Unauthorized for anonymous user")
    void addAnswer_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        Long feedbackId = 1L;
        FeedbackAnswerDTO answerDTO = new FeedbackAnswerDTO("Answer");

        mockMvc.perform(patch("/feedbacks/{id}/answer", feedbackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id}/answer should return 403 Forbidden if service throws AccessDenied")
    void addAnswer_shouldReturnForbidden_whenNotAuthorizedRole() throws Exception {

        Long feedbackId = 1L;
        FeedbackAnswerDTO answerDTO = new FeedbackAnswerDTO("Answer");

        when(feedbackService.addAnswerToFeedback(eq(feedbackId), eq("ROLE_CUSTOMER"), any(FeedbackAnswerDTO.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Тільки адмін може відповідати"));

        mockMvc.perform(patch("/feedbacks/{id}/answer", feedbackId)
                        .with(user("user").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /feedbacks/{id}/answer should return 404 Not Found when feedback exists")
    void addAnswer_shouldReturnNotFound_whenFeedbackDoesNotExist() throws Exception {
        Long feedbackId = 999L;
        FeedbackAnswerDTO answerDTO = new FeedbackAnswerDTO("Answer");

        when(feedbackService.addAnswerToFeedback(anyLong(), anyString(), any(FeedbackAnswerDTO.class)))
                .thenThrow(new EntityNotFoundException("Feedback not found"));

        mockMvc.perform(patch("/feedbacks/{id}/answer", feedbackId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /feedbacks/{id} should return 204 No Content for authorized user")
    void delete_shouldReturnNoContent() throws Exception {

        Long feedbackId = 1L;
        UUID userId = UUID.randomUUID();

        doNothing().when(feedbackService).deleteFeedback(eq(feedbackId), any(Authentication.class));

        mockMvc.perform(delete("/feedbacks/{id}", feedbackId)
                        .with(user(userId.toString()).roles("CUSTOMER")))
                .andExpect(status().isNoContent());

        verify(feedbackService, times(1)).deleteFeedback(eq(feedbackId), any(Authentication.class));
    }

    @Test
    @DisplayName("DELETE /feedbacks/{id} should return 401 Unauthorized for anonymous user")
    void delete_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        Long feedbackId = 1L;

        mockMvc.perform(delete("/feedbacks/{id}", feedbackId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("DELETE /feedbacks/{id} should return 403 Forbidden when service throws AccessDeniedException")
    void delete_shouldReturnForbidden_whenAccessDenied() throws Exception {

        Long feedbackId = 1L;
        UUID strangerId = UUID.randomUUID();

        doThrow(new AccessDeniedException("Access Denied"))
                .when(feedbackService).deleteFeedback(eq(feedbackId), any(Authentication.class));

        mockMvc.perform(delete("/feedbacks/{id}", feedbackId)
                        .with(user(strangerId.toString()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /feedbacks/{id} should return 404 Not Found when feedback does not exist")
    void delete_shouldReturnNotFound_whenFeedbackDoesNotExist() throws Exception {

        Long feedbackId = 999L;

        doThrow(new EntityNotFoundException("Feedback not found"))
                .when(feedbackService).deleteFeedback(eq(feedbackId), any(Authentication.class));

        mockMvc.perform(delete("/feedbacks/{id}", feedbackId)
                        .with(user(UUID.randomUUID().toString()).roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /feedbacks/user should return 200 OK with paged feedbacks for authorized user")
    void getFeedbacksByUserId_shouldReturnPagedFeedbacks() throws Exception {

        UUID userId = UUID.randomUUID();
        int page = 0;
        int size = 10;

        FeedbackResponseDTO feedbackDto = new FeedbackResponseDTO(
                1L, "Great service", 5, "Ivan", "url", OffsetDateTime.now(), null, null
        );
        Page<FeedbackResponseDTO> pagedResponse = new PageImpl<>(List.of(feedbackDto));

        when(feedbackService.getFeedbacksByUserId(eq(userId), eq(page), eq(size)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/feedbacks/my")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].comment").value("Great service"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(feedbackService).getFeedbacksByUserId(eq(userId), eq(page), eq(size));
    }

    @Test
    @DisplayName("GET /feedbacks/user should return 401 Unauthorized for anonymous user")
    void getFeedbacksByUserId_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        mockMvc.perform(get("/feedbacks/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("GET /feedbacks/user should return 400 BadRequest when pagination parameters are negative")
    void getFeedbacksByUserId_shouldReturnBadRequest_whenParamsInvalid() throws Exception {

        mockMvc.perform(get("/feedbacks/my")
                        .with(user(UUID.randomUUID().toString()).roles("CUSTOMER"))
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("GET /feedbacks/user should return 200 OK with empty content when user has no feedbacks")
    void getFeedbacksByUserId_shouldReturnEmptyPage_whenNoFeedbacks() throws Exception {

        UUID userId = UUID.randomUUID();
        Page<FeedbackResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());

        when(feedbackService.getFeedbacksByUserId(any(UUID.class), anyInt(), anyInt()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/feedbacks/my")
                        .with(user(userId.toString()).roles("CUSTOMER"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /feedbacks/product/{productId} should return 200 OK with paged feedbacks")
    void getFeedbacksByProductId_shouldReturnPagedFeedbacks() throws Exception {

        Long productId = 100L;
        int page = 0;
        int size = 5;

        FeedbackResponseDTO feedbackDto = new FeedbackResponseDTO(
                1L, "Чудовий товар!", 5, "Олена", "image_url", OffsetDateTime.now(), null, null
        );
        Page<FeedbackResponseDTO> pagedResponse = new PageImpl<>(List.of(feedbackDto));

        when(feedbackService.getFeedbacksByProductId(eq(productId), eq(page), eq(size)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/feedbacks/product/{productId}", productId)
                        .with(user("user").roles("CUSTOMER"))
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comment").value("Чудовий товар!"))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(feedbackService).getFeedbacksByProductId(eq(productId), eq(page), eq(size));
    }

    @Test
    @DisplayName("GET /feedbacks/product/{productId} should return 400 BadRequest when pagination params are negative")
    void getFeedbacksByProductId_shouldReturnBadRequest_whenParamsInvalid() throws Exception {

        Long productId = 100L;

        mockMvc.perform(get("/feedbacks/product/{productId}", productId)
                        .with(user("user").roles("CUSTOMER"))
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("GET /feedbacks/product/{productId} should return 401 Unauthorized for anonymous user")
    void getFeedbacksByProductId_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        mockMvc.perform(get("/feedbacks/product/100")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /feedbacks/product/{productId} should return 200 OK with empty page if no feedbacks found")
    void getFeedbacksByProductId_shouldReturnEmptyPage_whenNoData() throws Exception {

        Long productId = 999L;
        Page<FeedbackResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());

        when(feedbackService.getFeedbacksByProductId(anyLong(), anyInt(), anyInt()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/feedbacks/product/{productId}", productId)
                        .with(user("user").roles("CUSTOMER"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /feedbacks/store should return 200 OK with average rating and paged feedbacks")
    void getFeedbacksByStore_shouldReturnFullResponse() throws Exception {

        int page = 0;
        int size = 10;
        BigDecimal averageRating = new BigDecimal("4.5");

        FeedbackResponseDTO feedbackDto = new FeedbackResponseDTO(
                1L, "Класний магазин", 5, "Марія", "url",
                OffsetDateTime.now(), "Дякуємо", OffsetDateTime.now()
        );
        Page<FeedbackResponseDTO> pagedResponse = new PageImpl<>(List.of(feedbackDto));

        when(feedbackService.getFeedbacksAboutStore(page, size)).thenReturn(pagedResponse);
        when(feedbackService.getAverageRatingForStore()).thenReturn(averageRating);

        mockMvc.perform(get("/feedbacks/store")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeRating").value(4.5))
                .andExpect(jsonPath("$.feedbacks.content[0].comment").value("Класний магазин"))
                .andExpect(jsonPath("$.feedbacks.page.totalElements").value(1));

        verify(feedbackService).getFeedbacksAboutStore(page, size);
        verify(feedbackService).getAverageRatingForStore();
    }

    @Test
    @DisplayName("GET /feedbacks/store should work for anonymous users (PermitAll)")
    void getFeedbacksByStore_shouldBeAccessibleForAnonymous() throws Exception {

        when(feedbackService.getFeedbacksAboutStore(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(feedbackService.getAverageRatingForStore())
                .thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/feedbacks/store")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /feedbacks/store should return 400 when pagination parameters are invalid")
    void getFeedbacksByStore_shouldReturnBadRequest_whenParamsNegative() throws Exception {

        mockMvc.perform(get("/feedbacks/store")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @DisplayName("GET /feedbacks/store should return zero rating and empty page when no data exists")
    void getFeedbacksByStore_shouldHandleEmptyData() throws Exception {

        when(feedbackService.getFeedbacksAboutStore(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(feedbackService.getAverageRatingForStore())
                .thenReturn(new BigDecimal("0.0"));

        mockMvc.perform(get("/feedbacks/store")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeRating").value(0.0))
                .andExpect(jsonPath("$.feedbacks.content").isEmpty());
    }
}
