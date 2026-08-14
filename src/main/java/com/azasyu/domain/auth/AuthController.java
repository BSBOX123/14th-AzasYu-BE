package com.azasyu.domain.auth;

import com.azasyu.domain.auth.dto.AuthResponse;
import com.azasyu.domain.auth.dto.LoginRequest;
import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입과 로그인. 이 두 API만 토큰 없이 호출할 수 있다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "회원가입",
        description = """
            새 계정을 만들고 바로 사용할 수 있는 액세스 토큰을 함께 반환한다.
            가입 후 별도로 로그인을 호출할 필요가 없다.

            이메일은 대소문자를 구분하지 않으며 소문자로 저장된다.

            **성공 시 201 Created**

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 입력 형식이 올바르지 않음 | 400 | `INVALID_REQUEST` |
            | 이미 가입된 이메일 | 409 | `EMAIL_ALREADY_EXISTS` |
            """
    )
    @PostMapping("/signup")
    ResponseEntity<ApiResponse<AuthResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(authService.signUp(request)));
    }

    @Operation(
        summary = "로그인",
        description = """
            이메일과 비밀번호로 액세스 토큰을 발급받는다.

            이후 모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요하다.
            만료 시간은 응답의 `expiresInSeconds`로 확인한다.
            **토큰 재발급 API는 없으므로** 만료되면 다시 로그인해야 한다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 이메일 또는 비밀번호 불일치 | 401 | `INVALID_CREDENTIALS` |
            """
    )
    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
