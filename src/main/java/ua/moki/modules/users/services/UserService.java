package ua.moki.modules.users.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.*;

import java.util.UUID;

public interface UserService {

    UserResponseDTO createUser(UserCreateDTO userCreateDTO);
    UserResponseDTO createManager(UserCreateDTO userCreateDTO);
    UserResponseDTO updateUser(UUID publicId, UserUpdateDTO userUpdateDTO);
    void updateBlockStatus(UUID publicId, boolean isBlocked);
    void initiateEmailChange(UUID userId, EmailChangeRequestDTO dto);
    void confirmEmailChange(String token);
    void changePassword(UUID userId, PasswordChangeRequestDTO dto);
    void deleteUser(UUID publicId);
    User getUserById(Long id);
    User getActiveUserEntityByPublicId(UUID publicId);
    UserResponseDTO getActiveUserByPublicId(UUID publicId);

    Page<UserResponseDTO> getAllUser(Boolean isDeleted, Pageable pageable);
}
