package com.azasyu.domain.interview;

import com.azasyu.domain.interview.dto.InterviewSubmissionResponse;
import com.azasyu.domain.interview.dto.SubmitInterviewRequest;
import com.azasyu.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "사전 인터뷰 답변",
    description = """
        공통 질문에 대한 개인 답변 제출과, 답변에서 생성된 개인 아이디어 카드.
        카드는 익명 보드에 올라가므로 본인 답변 원문은 다른 참여자에게 노출되지 않는다.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/interview")
public class InterviewSubmissionController {

    private final InterviewSubmissionService submissionService;

    @Operation(
        summary = "인터뷰 답변 제출",
        description = """
            공통 질문 전체에 한 번씩 답변해 제출한다.
            제출과 동시에 개인 아이디어 카드 생성이 시작된다.

            **선행 조건** — 공통 질문 조회의 `generationStatus`가 `GENERATED`여야 한다.

            **모든 질문에 정확히 하나씩** 답변해야 한다. 일부만 보내거나
            같은 질문에 두 번 답하면 400이 반환된다.
            회의당 **1회만** 제출할 수 있고 수정 API는 없다.

            카드 생성에 실패해도 **제출 자체는 저장된다.** 이 경우
            `cardGenerationStatus`가 `FAILED`가 되며 카드 재생성 API로 복구한다.

            **성공 시 201 Created**

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 답변 누락 또는 질문 불일치 | 400 | `INCOMPLETE_ANSWERS` |
            | 같은 질문에 중복 답변 | 400 | `DUPLICATE_ANSWER` |
            | 회의 참여자가 아님 | 404 | `INTERVIEW_NOT_FOUND` |
            | 질문이 아직 준비되지 않음 | 409 | `QUESTIONS_NOT_READY` |
            | 이미 제출함 | 409 | `INTERVIEW_ALREADY_SUBMITTED` |
            """
    )
    @PostMapping("/submissions")
    ResponseEntity<ApiResponse<InterviewSubmissionResponse>> submit(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId,
        @Valid @RequestBody SubmitInterviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(submissionService.submit(userId, meetingId, request)));
    }

    @Operation(
        summary = "내 제출 내역 조회",
        description = """
            로그인한 사용자가 이 회의에 제출한 인터뷰와 생성된 아이디어 카드를 반환한다.
            제출 여부 확인과 카드 생성 상태 폴링에 쓴다.

            `cardGenerationStatus`가 `GENERATED`가 아니면 `ideaCard`는 `null`이다.

            | cardGenerationStatus | 의미 | 화면 처리 |
            |---|---|---|
            | `PENDING` | 생성 중 | 로딩 표시 후 잠시 뒤 재조회 |
            | `GENERATED` | 완료 | 카드 표시 |
            | `FAILED` | 실패 | `failureMessage` 표시 + 재생성 버튼 |
            | `NOT_CONFIGURED` | 서버에 Gemini 키 미설정 | 안내 문구 표시 |

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아님 | 404 | `INTERVIEW_NOT_FOUND` |
            | 아직 제출하지 않음 | 404 | `SUBMISSION_NOT_FOUND` |
            """
    )
    @GetMapping("/submissions/me")
    ApiResponse<InterviewSubmissionResponse> getMine(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(submissionService.getMine(userId, meetingId));
    }

    @Operation(
        summary = "내 아이디어 카드 재생성",
        description = """
            카드 생성에 실패했을 때 이미 제출한 답변으로 다시 시도한다.
            답변을 다시 보낼 필요는 없다.

            **이미 `GENERATED` 상태면 아무 것도 하지 않고 현재 카드를 그대로 반환한다.**

            **응답은 생성이 끝난 뒤 반환된다.** AI 응답을 기다리므로 수 초가 걸릴 수 있다.
            생성에 실패해도 HTTP 200이 반환되며, 실패 여부는 `cardGenerationStatus`로 판단한다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의 참여자가 아님 | 404 | `INTERVIEW_NOT_FOUND` |
            | 아직 제출하지 않음 | 404 | `SUBMISSION_NOT_FOUND` |
            """
    )
    @PostMapping("/submissions/me/idea-card/generate")
    ApiResponse<InterviewSubmissionResponse> retryCardGeneration(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(submissionService.retryCardGeneration(userId, meetingId));
    }
}
