package com.azasyu.domain.meeting;

import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.meeting.dto.MeetingDetailResponse;
import com.azasyu.domain.meeting.dto.MeetingSummaryResponse;
import com.azasyu.global.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/projects/{projectId}/meetings")
    ResponseEntity<ApiResponse<MeetingDetailResponse>> create(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long projectId,
        @Valid @RequestBody CreateMeetingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(meetingService.create(userId, projectId, request)));
    }

    @GetMapping("/projects/{projectId}/meetings")
    ApiResponse<List<MeetingSummaryResponse>> getProjectMeetings(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long projectId
    ) {
        return ApiResponse.success(meetingService.getProjectMeetings(userId, projectId));
    }

    @GetMapping("/meetings/{meetingId}")
    ApiResponse<MeetingDetailResponse> getDetail(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(meetingService.getDetail(userId, meetingId));
    }
}
