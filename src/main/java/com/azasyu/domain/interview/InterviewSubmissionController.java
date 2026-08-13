package com.azasyu.domain.interview;

import com.azasyu.domain.interview.dto.InterviewSubmissionResponse;
import com.azasyu.domain.interview.dto.SubmitInterviewRequest;
import com.azasyu.global.api.ApiResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/interview")
public class InterviewSubmissionController {

    private final InterviewSubmissionService submissionService;

    @PostMapping("/submissions")
    ResponseEntity<ApiResponse<InterviewSubmissionResponse>> submit(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId,
        @Valid @RequestBody SubmitInterviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(submissionService.submit(userId, meetingId, request)));
    }

    @GetMapping("/submissions/me")
    ApiResponse<InterviewSubmissionResponse> getMine(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(submissionService.getMine(userId, meetingId));
    }

    @PostMapping("/submissions/me/idea-card/generate")
    ApiResponse<InterviewSubmissionResponse> retryCardGeneration(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(submissionService.retryCardGeneration(userId, meetingId));
    }
}
