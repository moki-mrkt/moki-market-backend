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
    UserAdminResponseDTO updateUserByAdmin(UUID publicId, UserAdminUpdateDTO userAdminUpdateDTO);
    UserResponseDTO updateAvatar(UUID publicId, AvatarUpdateDTO avatarUpdateDTO);
    void updateBlockStatus(UUID publicId, boolean isBlocked);
    void deleteUser(UUID publicId);
    User getUserById(Long id);
    User getActiveUserEntityByPublicId(UUID publicId);
    UserResponseDTO getActiveUserByPublicId(UUID publicId);
    UserAdminResponseDTO getUserByPublicIdForAdmin(UUID publicId);

    Page<UserAdminResponseDTO> getAllUser(String query, Boolean isDeleted, Pageable pageable);
}
