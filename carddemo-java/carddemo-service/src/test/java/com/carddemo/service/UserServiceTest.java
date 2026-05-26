package com.carddemo.service;

import com.carddemo.domain.entity.UserSecurity;
import com.carddemo.domain.repository.UserSecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userSecurityRepository, passwordEncoder);
    }

    @Test
    void listUsersReturnsPaginatedResults() {
        UserSecurity user = UserSecurity.builder().usrId("USER01").usrType("U").usrPwd("hash").build();
        Page<UserSecurity> page = new PageImpl<>(List.of(user));
        Pageable pageable = PageRequest.of(0, 10);

        when(userSecurityRepository.findAll(pageable)).thenReturn(page);

        Page<UserSecurity> result = userService.listUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsrId()).isEqualTo("USER01");
    }

    @Test
    void getUserReturnsExistingUser() {
        UserSecurity user = UserSecurity.builder().usrId("USER01").usrType("U").usrPwd("hash").build();
        when(userSecurityRepository.findById("USER01")).thenReturn(Optional.of(user));

        UserSecurity result = userService.getUser("USER01");

        assertThat(result.getUsrId()).isEqualTo("USER01");
    }

    @Test
    void getUserThrowsForNonexistentUser() {
        when(userSecurityRepository.findById("NOUSER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("NOUSER"))
                .isInstanceOf(UserService.UserNotFoundException.class);
    }

    @Test
    void createUserHashesPassword() {
        when(userSecurityRepository.existsById("NEW01")).thenReturn(false);
        when(userSecurityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSecurity result = userService.createUser("NEW01", "First", "Last", "password123", "U");

        assertThat(result.getUsrId()).isEqualTo("NEW01");
        assertThat(result.getUsrFname()).isEqualTo("First");
        assertThat(result.getUsrLname()).isEqualTo("Last");
        assertThat(result.getUsrType()).isEqualTo("U");
        assertThat(passwordEncoder.matches("password123", result.getUsrPwd())).isTrue();
    }

    @Test
    void createDuplicateUserThrows() {
        when(userSecurityRepository.existsById("DUPE01")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser("DUPE01", "A", "B", "password1", "U"))
                .isInstanceOf(UserService.DuplicateUserException.class);
    }

    @Test
    void updateUserUpdatesFields() {
        UserSecurity existing = UserSecurity.builder()
                .usrId("USER01").usrFname("Old").usrLname("Name")
                .usrPwd("oldhash").usrType("U").build();
        when(userSecurityRepository.findById("USER01")).thenReturn(Optional.of(existing));
        when(userSecurityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSecurity result = userService.updateUser("USER01", "New", "Last", "newpass12", "A");

        assertThat(result.getUsrFname()).isEqualTo("New");
        assertThat(result.getUsrLname()).isEqualTo("Last");
        assertThat(result.getUsrType()).isEqualTo("A");
        assertThat(passwordEncoder.matches("newpass12", result.getUsrPwd())).isTrue();
    }

    @Test
    void updateUserWithNullFieldsPreservesExisting() {
        UserSecurity existing = UserSecurity.builder()
                .usrId("USER01").usrFname("First").usrLname("Last")
                .usrPwd("oldhash").usrType("U").build();
        when(userSecurityRepository.findById("USER01")).thenReturn(Optional.of(existing));
        when(userSecurityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSecurity result = userService.updateUser("USER01", null, null, null, null);

        assertThat(result.getUsrFname()).isEqualTo("First");
        assertThat(result.getUsrLname()).isEqualTo("Last");
        assertThat(result.getUsrPwd()).isEqualTo("oldhash");
        assertThat(result.getUsrType()).isEqualTo("U");
    }

    @Test
    void deleteUserSucceeds() {
        when(userSecurityRepository.existsById("USER01")).thenReturn(true);

        userService.deleteUser("USER01");

        verify(userSecurityRepository).deleteById("USER01");
    }

    @Test
    void deleteNonexistentUserThrows() {
        when(userSecurityRepository.existsById("NOUSER")).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser("NOUSER"))
                .isInstanceOf(UserService.UserNotFoundException.class);
    }
}
