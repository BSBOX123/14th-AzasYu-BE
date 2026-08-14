package com.azasyu.domain.interview;

import com.azasyu.domain.interview.dto.InterviewQuestionsResponse;
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
    name = "사전 인터뷰 질문",
    description = """
        회의 안건을 바탕으로 AI가 만든 공통 질문. 회의 생성 시 자동으로 생성되며
        모든 참여자가 같은 질문에 답변한다.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/interview/questions")
public class InterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @Operation(
        summary = "공통 질문 조회",
        description = """
            회의의 공통 질문 목록과 생성 상태를 반환한다.
            답변 제출 화면에 진입하기 전에 이 API로 상태를 확인해야 한다.

            **`generationStatus`가 `GENERATED`일 때만 답변을 제출할 수 있다.**
            그 외 상태에서는 `questions`가 빈 배열이다.

            | generationStatus | 의미 | 화면 처리 |
            |---|---|---|
            | `PENDING` | 생성 중 | 로딩 표시 후 잠시 뒤 재조회 |
            | `GENERATED` | 완료 | 질문 표시, 답변 입력 가능 |
            | `FAILED` | 실패 | `failureMessage` 표시 + 재생성 버튼 |
            | `NOT_CONFIGURED` | 서버에 Gemini 키 미설정 | 안내 문구 표시 |

            **회의 참여자만 조회할 수 있다.** 프로젝트 구성원이어도 회의 참여자가
            아니면 404가 반환된다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아님 | 404 | `INTERVIEW_NOT_FOUND` |
            | 질문 묶음이 아직 없음 | 404 | `INTERVIEW_NOT_FOUND` |
            """
    )
    @GetMapping
    ApiResponse<InterviewQuestionsResponse> getQuestions(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(interviewQuestionService.getQuestions(userId, meetingId));
    }

    @Operation(
        summary = "공통 질문 재생성",
        description = """
            질문 생성에 실패했을 때 다시 시도한다.

            **이미 `GENERATED` 상태면 아무 것도 하지 않고 현재 질문을 그대로 반환한다.**
            이미 답변한 참여자가 있는데 질문이 바뀌면 답변이 어긋나기 때문이다.

            **응답은 생성이 끝난 뒤 반환된다.** AI 응답을 기다리므로 수 초가 걸릴 수 있다.
            생성에 실패해도 HTTP 200이 반환되며, 실패 여부는 `generationStatus`로 판단한다.

            **회의 참여자가 아니라 프로젝트 구성원이면 호출할 수 있다.** 조회 API와 권한 기준이 다르다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의가 없거나 프로젝트 구성원이 아님 | 404 | `MEETING_NOT_FOUND` |
            """
    )
    @PostMapping("/generate")
    ApiResponse<InterviewQuestionsResponse> generate(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(interviewQuestionService.retry(userId, meetingId));
    }
}
