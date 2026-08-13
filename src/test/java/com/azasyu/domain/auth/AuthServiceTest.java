package com.azasyu.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azasyu.domain.auth.dto.LoginRequest;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.user.UserRepository;
import com.azasyu.global.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void signsUpAndLogsIn() {
        var signedUp = authService.signUp(
            new SignUpRequest("USER@example.com", "회의 참여자", "password123")
        );

        var loggedIn = authService.login(
            new LoginRequest("user@example.com", "password123")
        );

        assertThat(loggedIn.userId()).isEqualTo(signedUp.userId());
        assertThat(loggedIn.email()).isEqualTo("user@example.com");
        assertThat(loggedIn.accessToken()).isNotBlank();
    }

    @Test
    void rejectsDuplicateEmail() {
        var request = new SignUpRequest("duplicate@example.com", "사용자", "password123");
        authService.signUp(request);

        assertThatThrownBy(() -> authService.signUp(request))
            .isInstanceOf(ApiException.class)
            .hasMessage("이미 가입된 이메일입니다.");
    }
}
