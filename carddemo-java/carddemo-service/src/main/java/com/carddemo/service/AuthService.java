package com.carddemo.service;

import com.carddemo.domain.entity.UserSecurity;
import com.carddemo.domain.repository.UserSecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserSecurityRepository userSecurityRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public String authenticate(String userId, String password) {
        UserSecurity user = userSecurityRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getUsrPwd())) {
            throw new AuthenticationException("Invalid credentials");
        }

        return jwtTokenProvider.generateToken(
                user.getUsrId(),
                user.getUsrType(),
                user.getUsrFname(),
                user.getUsrLname()
        );
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
