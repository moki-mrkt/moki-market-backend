package ua.moki.modules.users.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ua.moki.modules.users.security.jwt.AccessTokenJwsStringDeserializer;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final AccessTokenJwsStringDeserializer accessTokenDeserializer;

    public JwtFilter(HandlerExceptionResolver handlerExceptionResolver,
                     AccessTokenJwsStringDeserializer accessTokenDeserializer) {
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.accessTokenDeserializer = accessTokenDeserializer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if(!request.getRequestURI().contains("/actuator/")) {
            log.info(request.getMethod() + " " + request.getRequestURI());
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String tokenString = authHeader.substring(7);

            Token token = accessTokenDeserializer.apply(tokenString);

            authenticateUser(token);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT Authentication failed: {}", e.getMessage());

            handlerExceptionResolver.resolveException(request, response, null, e);
        }

    }

    private void authenticateUser (Token token) {
        List<SimpleGrantedAuthority> authorities = token.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                token.subject(),
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
