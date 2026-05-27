package com.carddemo.service;

import com.carddemo.domain.entity.UserSecurity;
import com.carddemo.domain.repository.UserSecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac", 3600000L);
        authService = new AuthService(userSecurityRepository, jwtTokenProvider, passwordEncoder);
    }

    @Test
    void authenticateWithValidCredentials() {
        UserSecurity user = UserSecurity.builder()
                .usrId("ADMIN01")
                .usrFname("Admin")
                .usrLname("User")
                .usrPwd(passwordEncoder.encode("password123"))
                .usrType("A")
                .build();

        when(userSecurityRepository.findById("ADMIN01")).thenReturn(Optional.of(user));

        String token = authService.authenticate("ADMIN01", "password123");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo("ADMIN01");
        assertThat(jwtTokenProvider.getUserType(token)).isEqualTo("A");
    }

    @Test
    void authenticateWithInvalidPassword() {
        UserSecurity user = UserSecurity.builder()
                .usrId("USER01")
                .usrPwd(passwordEncoder.encode("correct"))
                .usrType("U")
                .build();

        when(userSecurityRepository.findById("USER01")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.authenticate("USER01", "wrong"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void authenticateWithNonexistentUser() {
        when(userSecurityRepository.findById("NOUSER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("NOUSER", "password"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessage("Invalid credentials");
    }
}
