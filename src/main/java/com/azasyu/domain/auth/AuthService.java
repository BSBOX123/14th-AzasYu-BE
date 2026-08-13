package com.azasyu.domain.auth;

import com.azasyu.domain.auth.dto.AuthResponse;
import com.azasyu.domain.auth.dto.LoginRequest;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.user.User;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import com.azasyu.global.security.JwtTokenProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
        }

        User user = userRepository.save(
            new User(email, request.name().trim(), passwordEncoder.encode(request.password()))
        );
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        return new AuthResponse(
            user.getId(), user.getEmail(), user.getName(), token, "Bearer",
            jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
