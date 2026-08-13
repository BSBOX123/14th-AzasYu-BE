package com.azasyu.domain.interview;

import com.azasyu.domain.interview.dto.InterviewQuestionsResponse;
import com.azasyu.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/interview/questions")
public class InterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @GetMapping
    ApiResponse<InterviewQuestionsResponse> getQuestions(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(interviewQuestionService.getQuestions(userId, meetingId));
    }

    @PostMapping("/generate")
    ApiResponse<InterviewQuestionsResponse> generate(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(interviewQuestionService.retry(userId, meetingId));
    }
}
