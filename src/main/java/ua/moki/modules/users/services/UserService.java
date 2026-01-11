package ua.moki.modules.users.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.UserCreateDTO;
import ua.moki.modules.users.dtos.UserResponseDTO;
import ua.moki.modules.users.dtos.UserUpdateDTO;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponseDTO createUser(UserCreateDTO userCreateDTO);
    UserResponseDTO updateUser(UUID publicId, UserUpdateDTO userUpdateDTO);
    void deleteUser(UUID publicId);
    User getUserById(Long id);
    User getActiveUserEntityByPublicId(UUID publicId);
    UserResponseDTO getActiveUserByPublicId(UUID publicId);

    Page<UserResponseDTO> getAllUser(Boolean isDeleted, Pageable pageable);
}
