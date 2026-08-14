package com.azasyu.domain.interview;

import com.azasyu.domain.interview.dto.AnonymousIdeaCardResponse;
import com.azasyu.domain.interview.dto.IdeaSummaryResponse;
import com.azasyu.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "아이디어 보드",
    description = """
        참여자들의 익명 아이디어 카드와 그것을 종합한 전체 의견 요약.
        카드에는 작성자 정보가 담기지 않는다.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}")
public class IdeaBoardController {

    private final IdeaBoardService ideaBoardService;

    @Operation(
        summary = "익명 아이디어 카드 목록",
        description = """
            회의 참여자들이 제출한 아이디어 카드를 생성 시각 순으로 반환한다.

            **작성자를 식별할 수 있는 값이 응답에 포함되지 않는다.** 익명성이 이 기능의 핵심이므로
            화면에서도 순서나 개수로 작성자를 추정할 수 있게 표시하지 않는 편이 좋다.

            아직 카드가 없으면 빈 배열을 반환한다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아님 | 404 | `IDEA_BOARD_NOT_FOUND` |
            """
    )
    @GetMapping("/idea-cards")
    ApiResponse<List<AnonymousIdeaCardResponse>> getCards(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(ideaBoardService.getCards(userId, meetingId));
    }

    @Operation(
        summary = "최신 전체 의견 요약 조회",
        description = """
            가장 최근에 생성된 요약을 반환한다.

            **아직 한 번도 새로고침하지 않았다면 `data`가 `null`이다.**
            404가 아니라 `{"success": true, "data": null}` 형태이므로 화면에서 빈 상태로 처리한다.

            요약은 자동 생성되지 않는다. 새로고침 API를 호출해야 만들어진다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아님 | 404 | `IDEA_BOARD_NOT_FOUND` |
            """
    )
    @GetMapping("/idea-summary")
    ApiResponse<IdeaSummaryResponse> getLatestSummary(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(ideaBoardService.getLatestSummary(userId, meetingId));
    }

    @Operation(
        summary = "전체 의견 요약 새로고침",
        description = """
            현재 시점의 아이디어 카드 전체를 AI가 다시 종합해 **새 버전으로 저장한다.**
            기존 요약을 덮어쓰지 않고 `version`이 1씩 올라가며 누적된다.

            **응답은 생성이 끝난 뒤 반환된다.** AI 응답을 기다리므로 수 초가 걸릴 수 있다.

            다른 AI 기능과 달리 **실패하면 상태가 아니라 오류로 반환된다.**
            `status` 필드가 없으므로 HTTP 상태 코드로 판단해야 한다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아님 | 404 | `IDEA_BOARD_NOT_FOUND` |
            | 회의를 찾을 수 없음 | 404 | `MEETING_NOT_FOUND` |
            | 요약할 카드가 하나도 없음 | 409 | `NO_IDEA_CARDS` |
            | 서버에 Gemini 키 미설정 | 503 | `GEMINI_NOT_CONFIGURED` |
            | AI 생성 실패 | 502 | `IDEA_SUMMARY_FAILED` |
            """
    )
    @PostMapping("/idea-summary/refresh")
    ApiResponse<IdeaSummaryResponse> refreshSummary(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(ideaBoardService.refreshSummary(userId, meetingId));
    }
}
