package com.azasyu.domain.meeting.controller;

import com.azasyu.domain.meeting.dto.MeetingAnalysisResponse;
import com.azasyu.domain.meeting.service.MeetingAnalysisService;
import com.azasyu.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "회의 분석",
    description = """
        회의 원문에서 AI가 뽑아낸 요약과 모호한 표현 탐지 결과.
        원문 등록 시 자동으로 생성되며, 실패하면 재생성 API로 다시 시도한다.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/result")
public class MeetingAnalysisController {

    private final MeetingAnalysisService analysisService;

    @Operation(
        summary = "회의 분석 결과 조회",
        description = """
            회의 목적, 주요 논의, 결정 사항, 추가 확인 항목과
            모호한 표현 목록을 반환한다.

            **`status`를 먼저 확인해야 한다.** `GENERATED`가 아니면 본문 필드가 모두 `null`이다.

            | status | 의미 | 화면 처리 |
            |---|---|---|
            | `PENDING` | 생성 중 | 로딩 표시 후 잠시 뒤 재조회 |
            | `GENERATED` | 완료 | 결과 표시 |
            | `FAILED` | 실패 | `failureMessage` 표시 + 재생성 버튼 |
            | `NOT_CONFIGURED` | 서버에 Gemini 키 미설정 | 안내 문구 표시 |

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아니거나 회의 없음 | 404 | `MEETING_NOT_FOUND` |
            | 아직 분석이 만들어지지 않음(원문 미등록) | 404 | `MEETING_ANALYSIS_NOT_FOUND` |
            """
    )
    @GetMapping
    ApiResponse<MeetingAnalysisResponse> get(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(analysisService.get(userId, meetingId));
    }

    @Operation(
        summary = "회의 분석 재생성",
        description = """
            분석을 다시 생성한다. 기존 결과와 모호성 목록은 새 결과로 교체된다.
            `GENERATED` 상태에서 호출해도 다시 생성한다.

            **응답은 생성이 끝난 뒤 반환된다.** AI 응답을 기다리므로 수 초가 걸릴 수 있다.
            생성에 실패해도 HTTP 200이 반환되며, 실패 여부는 `status`로 판단한다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아니거나 회의 없음 | 404 | `MEETING_NOT_FOUND` |
            | 회의 원문이 아직 등록되지 않음 | 409 | `MEETING_RECORD_REQUIRED` |
            """
    )
    @PostMapping("/generate")
    ApiResponse<MeetingAnalysisResponse> generate(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(analysisService.retry(userId, meetingId));
    }
}
