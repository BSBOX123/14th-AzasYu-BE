package com.azasyu.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignUpRequest(
    @Schema(description = "이메일. 대소문자를 구분하지 않는다.", example = "hong@example.com")
    @NotBlank @Email String email,

    @Schema(description = "표시 이름 (최대 100자)", example = "홍길동")
    @NotBlank @Size(max = 100) String name,

    @Schema(description = "비밀번호 (8~72자)", example = "password123")
    @NotBlank @Size(min = 8, max = 72) String password
) {
}
