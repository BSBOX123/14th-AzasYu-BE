package com.azasyu.domain.meeting;

import com.azasyu.domain.meeting.dto.MeetingAnalysisResponse;
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
@RequestMapping("/api/v1/meetings/{meetingId}/result")
public class MeetingAnalysisController {

    private final MeetingAnalysisService analysisService;

    @GetMapping
    ApiResponse<MeetingAnalysisResponse> get(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(analysisService.get(userId, meetingId));
    }

    @PostMapping("/generate")
    ApiResponse<MeetingAnalysisResponse> generate(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(analysisService.retry(userId, meetingId));
    }
}
