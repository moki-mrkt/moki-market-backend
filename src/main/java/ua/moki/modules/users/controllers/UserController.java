package ua.moki.modules.users.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.moki.modules.users.dtos.*;
import ua.moki.modules.users.services.UserService;

import java.net.URI;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    @SecurityRequirements()
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid UserCreateDTO dto) {

        UserResponseDTO userResponseDTO =  userService.createUser(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/users/{id}")
                .buildAndExpand(userResponseDTO.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(userResponseDTO);
    }

    @PostMapping("/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createManager(@RequestBody @Valid UserCreateDTO dto) {

        UserResponseDTO userResponseDTO = userService.createManager(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDTO);
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(Principal principal,
                                                      @Valid @RequestBody UserUpdateDTO userUpdateDTO) {

        UUID userId = UUID.fromString(principal.getName());

        UserResponseDTO updatedUser = userService.updateUser(userId, userUpdateDTO);

        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/profile/avatar")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<UserResponseDTO> updateAvatar(Principal principal,
                                                        @RequestBody AvatarUpdateDTO dto) {
        UUID userId = UUID.fromString(principal.getName());
        UserResponseDTO updatedUser = userService.updateAvatar(userId, dto);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserAdminResponseDTO> updateUserByAdmin(@PathVariable UUID id,
                                                             @RequestBody UserAdminUpdateDTO userAdminUpdateDTO) {
        UserAdminResponseDTO updatedUser = userService.updateUserByAdmin(id, userAdminUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/block-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> switchBlockStatusUser(@PathVariable UUID id, @RequestParam boolean isBlocked) {
        userService.updateBlockStatus(id, isBlocked);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteUserByAdmin(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteCurrentAccount(@NotNull Principal principal) {
        userService.deleteUser(UUID.fromString(principal.getName()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(Principal principal) {
        UserResponseDTO user = userService.getActiveUserByPublicId(UUID.fromString(principal.getName()));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<UserAdminResponseDTO>> getAllUsers(
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<UserAdminResponseDTO> users = userService.getAllUser(deleted, PageRequest.of(page, size));
        return ResponseEntity.ok(users);
    }

}
