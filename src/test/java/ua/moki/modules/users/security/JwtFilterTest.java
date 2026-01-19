package ua.moki.modules.users.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringDeserializer;
import ua.moki.util.exceptions.InvalidTokenException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtFilterTest {

    @Mock
    private HandlerExceptionResolver exceptionResolver;
    @Mock
    private AccessTokenJwsStringDeserializer deserializer;
    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(exceptionResolver, deserializer);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal skips the request further if the Authorization header is missing")
    void doFilterInternal_shouldContinueChain_whenNoHeaderPresent() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(deserializer);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("doFilterInternal authenticates the user if the token is valid")
    void doFilterInternal_shouldAuthenticateUser_whenTokenIsValid() throws Exception {

        String tokenString = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenString);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Token token = new Token(
                UUID.randomUUID().toString(),
                List.of("ROLE_USER"),
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        when(deserializer.apply(tokenString)).thenReturn(token);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(deserializer).apply(tokenString);
        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(token.subject());
    }

    @Test
    @DisplayName("doFilterInternal calls ExceptionResolver if the token is invalid")
    void doFilterInternal_shouldDelegateException_whenTokenIsInvalid() throws Exception {

        String invalidToken = "invalid.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        RuntimeException ex = new InvalidTokenException("Invalid signature");

        when(deserializer.apply(invalidToken)).thenThrow(ex);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(exceptionResolver).resolveException(eq(request), eq(response), eq(null), eq(ex));

        verify(filterChain, never()).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
