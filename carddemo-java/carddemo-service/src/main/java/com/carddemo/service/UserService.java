package com.carddemo.service;

import com.carddemo.domain.entity.UserSecurity;
import com.carddemo.domain.repository.UserSecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserSecurityRepository userSecurityRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserSecurity> listUsers(Pageable pageable) {
        return userSecurityRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public UserSecurity getUser(String userId) {
        return userSecurityRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public UserSecurity createUser(String userId, String firstName, String lastName,
                                   String password, String userType) {
        if (userSecurityRepository.existsById(userId)) {
            throw new DuplicateUserException(userId);
        }

        UserSecurity user = UserSecurity.builder()
                .usrId(userId)
                .usrFname(firstName)
                .usrLname(lastName)
                .usrPwd(passwordEncoder.encode(password))
                .usrType(userType)
                .build();

        return userSecurityRepository.save(user);
    }

    @Transactional
    public UserSecurity updateUser(String userId, String firstName, String lastName,
                                   String password, String userType) {
        UserSecurity user = userSecurityRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (firstName != null) {
            user.setUsrFname(firstName);
        }
        if (lastName != null) {
            user.setUsrLname(lastName);
        }
        if (password != null && !password.isBlank()) {
            user.setUsrPwd(passwordEncoder.encode(password));
        }
        if (userType != null) {
            user.setUsrType(userType);
        }

        return userSecurityRepository.save(user);
    }

    @Transactional
    public void deleteUser(String userId) {
        if (!userSecurityRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        userSecurityRepository.deleteById(userId);
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String userId) {
            super("User not found: " + userId);
        }
    }

    public static class DuplicateUserException extends RuntimeException {
        public DuplicateUserException(String userId) {
            super("User already exists: " + userId);
        }
    }
}
