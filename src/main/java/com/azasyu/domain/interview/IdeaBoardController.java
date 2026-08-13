package com.azasyu.domain.interview;

import com.azasyu.domain.interview.dto.AnonymousIdeaCardResponse;
import com.azasyu.domain.interview.dto.IdeaSummaryResponse;
import com.azasyu.global.api.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}")
public class IdeaBoardController {

    private final IdeaBoardService ideaBoardService;

    @GetMapping("/idea-cards")
    ApiResponse<List<AnonymousIdeaCardResponse>> getCards(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(ideaBoardService.getCards(userId, meetingId));
    }

    @GetMapping("/idea-summary")
    ApiResponse<IdeaSummaryResponse> getLatestSummary(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(ideaBoardService.getLatestSummary(userId, meetingId));
    }

    @PostMapping("/idea-summary/refresh")
    ApiResponse<IdeaSummaryResponse> refreshSummary(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(ideaBoardService.refreshSummary(userId, meetingId));
    }
}
