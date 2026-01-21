package ua.moki.modules.users.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.repositories.UserRepository;
import ua.moki.modules.users.utils.enums.RoleType;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("loadUserByUsername returns UserDetails if the user exists and has not been deleted")
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {

        String email = "test@mail.com";
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setEmail(email);
        user.setPassword("hashed_password");
        user.setRoleType(RoleType.CUSTOMER);
        user.setDeleted(false);

        when(userRepository.findByEmailAndDeletedFalse(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException if user is not found")
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {

        String email = "ghost@mail.com";
        when(userRepository.findByEmailAndDeletedFalse(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
