package com.azasyu.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignUpRequest(
    @Schema(description = "이메일. 대소문자를 구분하지 않는다.", example = "hong@example.com")
    @NotBlank(message = "이메일을 입력해 주세요")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    String email,

    @Schema(description = "표시 이름 (최대 100자)", example = "홍길동")
    @NotBlank(message = "이름을 입력해 주세요")
    @Size(max = 100, message = "이름은 100자 이하로 입력해 주세요")
    String name,

    @Schema(description = "비밀번호 (8~72자)", example = "password123")
    @NotBlank(message = "비밀번호를 입력해 주세요")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해 주세요")
    String password
) {
}
