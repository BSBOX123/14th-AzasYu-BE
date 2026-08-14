package com.azasyu.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String DESCRIPTION = """
        가짜 합의를 줄이기 위한 AI 기반 회의 지원 API.

        ## 공통 응답 형식

        모든 응답은 아래 형태로 감싸져 있다. 실제 데이터는 `data` 안에 있다.

        ```json
        { "success": true, "data": { ... } }
        ```

        실패하면 `data` 대신 `error`가 온다. **화면 분기는 HTTP 상태 코드가 아니라
        `error.code`로 하는 것이 안전하다.** 같은 상태 코드가 여러 상황에서 쓰이기 때문이다.

        ```json
        { "success": false, "error": { "code": "MEETING_NOT_FOUND", "message": "회의를 찾을 수 없습니다." } }
        ```

        `message`는 사용자에게 그대로 보여줄 수 있는 한국어 문구다.

        ## 인증

        `회원가입`과 `로그인`을 제외한 모든 API는 액세스 토큰이 필요하다.

        ```
        Authorization: Bearer {accessToken}
        ```

        우측 상단 **Authorize** 버튼에 토큰을 넣으면 이 문서에서 바로 호출해볼 수 있다.
        토큰 재발급 API는 없으므로 만료되면 다시 로그인해야 한다.

        ## AI 기능의 상태 필드

        공통 질문, 아이디어 카드, 회의 분석은 AI가 생성하며 **응답에 상태 필드가 함께 온다.**
        생성에 실패해도 HTTP 200이 반환되므로 상태 필드를 반드시 확인해야 한다.

        | 값 | 의미 |
        |---|---|
        | `PENDING` | 생성 중 |
        | `GENERATED` | 완료. 이때만 본문 데이터가 채워진다 |
        | `FAILED` | 실패. `failureMessage`를 보여주고 재생성 API로 복구한다 |
        | `NOT_CONFIGURED` | 서버에 Gemini API 키가 설정되지 않음 |

        예외적으로 **전체 의견 요약 새로고침**은 상태 필드 없이 오류로 반환한다.

        ## 권한 규칙

        존재 여부를 숨기기 위해 권한이 없는 리소스는 403이 아니라 **404**를 반환한다.
        프로젝트 구성원과 회의 참여자는 다른 개념이며, API마다 요구하는 기준이 다르므로
        각 API 설명을 확인한다.
        """;

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AzasYu API")
                .description(DESCRIPTION)
                .version("v1"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
