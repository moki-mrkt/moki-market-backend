package ua.moki.modules.users.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.UserCreateDTO;
import ua.moki.modules.users.dtos.UserResponseDTO;
import ua.moki.modules.users.dtos.UserUpdateDTO;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.services.UserService;
import ua.moki.modules.users.utils.mappers.UserMapper;
import ua.moki.util.exceptions.EntityNotFoundException;
import ua.moki.util.exceptions.UserAlreadyExistsException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserMapper userMapper,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {

        if (userRepository.existsByEmail(userCreateDTO.email())) {
            throw new UserAlreadyExistsException("Email already taken");
        }

        User user = new User();

        user.setFirstName(userCreateDTO.firstName());
        user.setSecondName(userCreateDTO.secondName());
        user.setEmail(userCreateDTO.email());
        user.setPhoneNumber(userCreateDTO.phoneNumber());

        user.setRoleType(RoleType.CUSTOMER);

        user.setPassword(passwordEncoder.encode(userCreateDTO.password()));

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(UUID publicId, UserUpdateDTO userUpdateDTO) {

        User user = this.getActiveUserEntityByPublicId(publicId);

        userMapper.updateUserFromDto(userUpdateDTO, user);

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID publicId) {

        User user = this.getActiveUserEntityByPublicId(publicId);

        user.setDeleted(true);
        user.setActivated(false);
        user.setBlocked(true);

        String deletedSuffix = "_deleted_" + Instant.now().getEpochSecond();

        user.setEmail(user.getEmail() + deletedSuffix);
        user.setPhoneNumber(user.getPhoneNumber() + deletedSuffix);

        user.setFirstName("Deleted User");
        user.setSecondName("");
        user.setPassword("");
        user.setImageId(null);

        user.setDateOfBirth(null);
        user.setAccessToAccount(false);
        user.setSubscribedToNews(false);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public User getActiveUserEntityByPublicId(UUID publicId) {
        return userRepository.findByPublicIdAndDeletedFalse(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id [%s]"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getActiveUserByPublicId(UUID publicId) {
        return userMapper.toResponseDTO(getActiveUserEntityByPublicId(publicId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUser(Boolean isDeleted, Pageable pageable) {

        Page<User> userPage = isDeleted == null
                ? userRepository.findAll(pageable)
                : userRepository.findAllByDeleted(isDeleted, pageable);

        return userPage.map(userMapper::toResponseDTO);
    }
}
