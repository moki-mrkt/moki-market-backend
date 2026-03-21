package ua.moki.modules.users.services.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.infrastructure.storage.service.FileStorageService;
import ua.moki.modules.sender.services.events.VerifyEmailEvent;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.UserDeliveryInfo;
import ua.moki.modules.users.dtos.*;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.services.UserSpecifications;
import ua.moki.modules.users.services.tokens.ActivationTokenService;
import ua.moki.modules.users.services.tokens.EmailTokenService;
import ua.moki.modules.users.services.RefreshTokenService;
import ua.moki.modules.users.services.UserService;
import ua.moki.modules.users.utils.enums.RoleType;
import ua.moki.modules.users.utils.mappers.UserMapper;
import ua.moki.util.exceptions.EntityNotFoundException;
import ua.moki.util.exceptions.UserAlreadyExistsException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserMapper userMapper;
    UserRepository userRepository;
    RefreshTokenService refreshTokenService;
    ActivationTokenService activationTokenService;
    PasswordEncoder passwordEncoder;
    FileStorageService fileStorageService;
    UserSpecifications userSpecifications;

    ApplicationEventPublisher eventPublisher;

    @Autowired
    public UserServiceImpl(UserMapper userMapper,
                           UserRepository userRepository,
                           RefreshTokenService refreshTokenService,
                           ActivationTokenService activationTokenService,
                           PasswordEncoder passwordEncoder, FileStorageService fileStorageService,
                           UserSpecifications userSpecifications,
                           ApplicationEventPublisher eventPublisher) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.activationTokenService = activationTokenService;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.userSpecifications = userSpecifications;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {

        User user = createUserEntity(userCreateDTO);

        user.setRoleType(RoleType.CUSTOMER);

        User savedUser = userRepository.save(user);

        String token = activationTokenService.generateToken(savedUser);

        eventPublisher.publishEvent(
                new VerifyEmailEvent(user.getEmail(), token)
        );

        return userMapper.toResponseDTO(savedUser);
    }

    @Override
    public UserResponseDTO createManager(UserCreateDTO userCreateDTO) {

        User user = createUserEntity(userCreateDTO);

        user.setRoleType(RoleType.MANAGER);

        user.setActivated(true);
        user.setAccessToAccount(true);
        user.setBlocked(false);

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDTO(savedUser);
    }

    private User createUserEntity(UserCreateDTO userCreateDTO) {

        if (userRepository.existsByEmail(userCreateDTO.email())) {
            throw new UserAlreadyExistsException("Email already taken");
        }

        User user = new User();
        user.setFirstName(userCreateDTO.firstName());
        user.setSecondName(userCreateDTO.secondName());
        user.setEmail(userCreateDTO.email());
        user.setPhoneNumber(userCreateDTO.phoneNumber());

        user.setPassword(passwordEncoder.encode(userCreateDTO.password()));

        return user;
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(UUID publicId, UserUpdateDTO userUpdateDTO) {

        User user = getActiveUserEntityByPublicId(publicId);

        if (user.getDeliveryInfo() == null) {
            user.setDeliveryInfo(new UserDeliveryInfo());
        }

        userMapper.updateUserFromDto(userUpdateDTO, user);

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public UserAdminResponseDTO updateUserByAdmin(UUID publicId, UserAdminUpdateDTO userAdminUpdateDTO) {

        User user = getActiveUserEntityByPublicId(publicId);

        userMapper.updateUserFromAdminDto(userAdminUpdateDTO, user);

        User savedUser = userRepository.save(user);

        return userMapper.toAdminResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO updateAvatar(UUID publicId, AvatarUpdateDTO avatarUpdateDTO) {

        User user = getActiveUserEntityByPublicId(publicId);

        String oldImageId = user.getImageId();

        user.setImageId(avatarUpdateDTO.imageId());
        User savedUser = userRepository.save(user);

        if (oldImageId != null && !oldImageId.equals(avatarUpdateDTO.imageId())) {
                fileStorageService.delete(oldImageId);
        }

        return userMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public void updateBlockStatus(UUID publicId, boolean isBlocked) {

        User user = getActiveUserEntityByPublicId(publicId);

        if (user.isBlocked() == isBlocked) return;

        user.setBlocked(isBlocked);
        userRepository.save(user);

        if (isBlocked) {
            refreshTokenService.deleteAllForUser(user.getId());
            log.info("User {} was BLOCKED and all tokens revoked.", publicId);
        } else {
            log.info("User {} was UNBLOCKED.", publicId);
        }
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
                .orElseThrow(() -> new EntityNotFoundException("User not found with id [%s]".formatted(publicId)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getActiveUserByPublicId(UUID publicId) {
        return userMapper.toResponseDTO(getActiveUserEntityByPublicId(publicId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminResponseDTO getUserByPublicIdForAdmin(UUID publicId) {
        return userMapper.toAdminResponseDTO(getActiveUserEntityByPublicId(publicId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserAdminResponseDTO> getAllUser(String query, Boolean isDeleted, Pageable pageable) {

        Specification<User> spec = userSpecifications.getSpecifications(query, isDeleted);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        return userPage.map(userMapper::toAdminResponseDTO);
    }
}
