package com.azasyu.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 결과와 액세스 토큰")
public record AuthResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long userId,

    @Schema(description = "가입한 이메일. 소문자로 정규화되어 저장된다.", example = "hong@example.com")
    String email,

    @Schema(description = "표시 이름", example = "홍길동")
    String name,

    @Schema(description = "JWT 액세스 토큰. 이후 요청의 Authorization 헤더에 담는다.",
        example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIn0.xxxxx")
    String accessToken,

    @Schema(description = "토큰 타입. Authorization 헤더는 `Bearer {accessToken}` 형식으로 만든다.",
        example = "Bearer")
    String tokenType,

    @Schema(description = "토큰 만료까지 남은 시간(초). 만료되면 재발급 없이 다시 로그인해야 한다.",
        example = "3600")
    long expiresInSeconds
) {
}
