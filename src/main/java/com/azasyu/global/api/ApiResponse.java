package com.azasyu.global.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
    모든 API의 공통 응답 봉투. 성공하면 data, 실패하면 error 중 하나만 채워진다.
    """)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    @Schema(description = "요청 성공 여부", example = "true")
    boolean success,

    @Schema(description = "성공 시 실제 데이터. 실패하면 생략된다.")
    T data,

    @Schema(description = "실패 시 오류 정보. 성공하면 생략된다.")
    ApiError error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }

    @Schema(description = "오류 정보. 화면 분기는 HTTP 상태 코드보다 code로 하는 것이 안전하다.")
    public record ApiError(
        @Schema(description = "오류 코드. 각 API 설명의 오류 표에서 의미를 확인한다.",
            example = "MEETING_NOT_FOUND")
        String code,

        @Schema(description = "사용자에게 그대로 보여줄 수 있는 한국어 메시지",
            example = "회의를 찾을 수 없습니다.")
        String message
    ) {
    }
}
