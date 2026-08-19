package com.azasyu.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
    /*
     * SignUpRequest와 달리 최상위 도메인을 요구하지 않음 — 규칙을 조이기 전에 가입한 계정이
     * 로그인하지 못하게 되기 때문. 여기서의 검증은 형식을 강제하는 것이 아니라
     * 조회할 가치가 없는 값을 걸러내는 용도임.
     */
    @Schema(description = "가입한 이메일", example = "hong@example.com")
    @NotBlank(message = "이메일을 입력해 주세요")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,

    @Schema(description = "비밀번호", example = "password123")
    @NotBlank(message = "비밀번호를 입력해 주세요")
    String password
) {
}
