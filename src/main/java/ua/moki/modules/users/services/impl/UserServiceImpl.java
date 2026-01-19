package ua.moki.modules.users.services.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.dtos.*;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.services.EmailTokenService;
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
    EmailTokenService emailTokenService;
    PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserMapper userMapper,
                           UserRepository userRepository,
                           RefreshTokenService refreshTokenService,
                           EmailTokenService emailTokenService,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.emailTokenService = emailTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {

        User user = createUserEntity(userCreateDTO);

        user.setRoleType(RoleType.CUSTOMER);

        User savedUser = userRepository.save(user);

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

        User user = this.getActiveUserEntityByPublicId(publicId);

        userMapper.updateUserFromDto(userUpdateDTO, user);

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional
    public void updateBlockStatus(UUID publicId, boolean isBlocked) {

        User user = getActiveUserEntityByPublicId(publicId);

        if (user.isBlocked() == isBlocked) {
            return;
        }

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
    public void initiateEmailChange(UUID userId, EmailChangeRequestDTO dto) {

        User user = getActiveUserEntityByPublicId(userId);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        if (userRepository.existsByEmail(dto.newEmail())) {
            throw new UserAlreadyExistsException("Email already taken");
        }

        String token = emailTokenService.generateEmailChangeToken(userId, dto.newEmail());

        //write unit tests
        // notifcationService.sendEmailAboutChaningEmail()
        // notificationService.sendEmailConfirmation(dto.newEmail(), token);
    }

    @Override
    @Transactional
    public void confirmEmailChange(String token) {

        var claims = emailTokenService.parseToken(token);

        User user = getActiveUserEntityByPublicId(claims.userId());

        if (userRepository.existsByEmail(claims.newEmail())) {
            throw new UserAlreadyExistsException("Email already taken");
        }

        user.setEmail(claims.newEmail());
        userRepository.save(user);

        refreshTokenService.deleteAllForUser(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequestDTO dto) {

        User user = getActiveUserEntityByPublicId(userId);

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid current password");
        }

        if (passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as old password");
        }

        user.setPassword(passwordEncoder.encode(dto.password()));
        userRepository.save(user);

        refreshTokenService.deleteAllForUser(user.getId());
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
    public Page<UserResponseDTO> getAllUser(Boolean isDeleted, Pageable pageable) {

        Page<User> userPage = isDeleted == null
                ? userRepository.findAll(pageable)
                : userRepository.findAllByDeleted(isDeleted, pageable);

        return userPage.map(userMapper::toResponseDTO);
    }
}
