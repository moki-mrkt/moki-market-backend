package ua.moki.modules.users.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ua.moki.modules.users.dtos.EmailChangeRequestDTO;
import ua.moki.modules.users.dtos.PasswordChangeRequestDTO;
import ua.moki.modules.users.dtos.UserResponseDTO;
import ua.moki.modules.users.dtos.auth.PasswordResetDTO;
import ua.moki.modules.users.dtos.auth.VerifyEmailDTO;
import ua.moki.modules.users.dtos.auth.VerifyOtpDTO;
import ua.moki.modules.users.services.AccountSecurityService;
import ua.moki.modules.users.services.UserService;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AccountSecurityController {

    private final AccountSecurityService accountSecurityService;

    @PostMapping("/activation")
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<Void> activateUser(@RequestParam String token) {

        accountSecurityService.activateUser(token);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/profile/email")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> changeEmail(Principal principal,
                                            @RequestBody @Valid EmailChangeRequestDTO request) {

        UUID userId = UUID.fromString(principal.getName());

        accountSecurityService.initiateEmailChange(userId, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/profile/email/confirm")
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<Void> confirmChange(@RequestParam String token) {
        accountSecurityService.confirmEmailChange(token);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/profile/password")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<UserResponseDTO> changePassword(Principal principal, @RequestBody @Valid PasswordChangeRequestDTO request) {

        UUID userId = UUID.fromString(principal.getName());

        accountSecurityService.changePassword(userId, request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/initiate")
    @SecurityRequirements()
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> initiatePasswordReset(@RequestBody @Valid VerifyEmailDTO verifyEmailDTO) {

        accountSecurityService.initiateForgotPassword(verifyEmailDTO.email());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/verify")
    @SecurityRequirements()
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody @Valid VerifyOtpDTO request) {

        String token = accountSecurityService.verifyOtpAndGetResetToken(request.otpCode());

        return ResponseEntity.ok(Map.of("resetToken", token));
    }

    @PostMapping("/password-reset/confirm")
    @PreAuthorize("hasAuthority('OP_RESET_PASSWORD')")
    public ResponseEntity<Void> confirmPasswordReset(Principal principal,
                                                     @RequestBody @Valid PasswordResetDTO passwordResetDTO) {

        UUID userId = UUID.fromString(principal.getName());

        accountSecurityService.resetPasswordWithJwt(userId, passwordResetDTO);

        return ResponseEntity.ok().build();
    }

}
