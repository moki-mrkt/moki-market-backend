package ua.moki.modules.users.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.moki.modules.users.dtos.auth.LogoutRequestDTO;
import ua.moki.modules.users.services.impl.AuthServiceImpl;
import ua.moki.modules.users.dtos.auth.AuthResponseDTO;
import ua.moki.modules.users.dtos.auth.LoginRequestDTO;
import ua.moki.modules.users.dtos.auth.RefreshTokenRequestDTO;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirements()
@PreAuthorize("permitAll()")
public class AuthController {

    private final AuthServiceImpl authServiceImpl;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authServiceImpl.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody @Valid RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(authServiceImpl.refreshAccessToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequestDTO request) {
        authServiceImpl.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
