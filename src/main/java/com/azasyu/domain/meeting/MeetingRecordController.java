package com.azasyu.domain.meeting;

import com.azasyu.domain.meeting.dto.CreateMeetingRecordRequest;
import com.azasyu.domain.meeting.dto.MeetingRecordResponse;
import com.azasyu.global.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/record")
public class MeetingRecordController {

    private final MeetingRecordService recordService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MeetingRecordResponse>> createFromText(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId,
        @Valid @RequestBody CreateMeetingRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(recordService.createFromText(userId, meetingId, request.content())));
    }

    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<MeetingRecordResponse>> createFromFile(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(recordService.createFromFile(userId, meetingId, file)));
    }

    @GetMapping
    ApiResponse<MeetingRecordResponse> get(
        @AuthenticationPrincipal Long userId, @PathVariable Long meetingId
    ) {
        return ApiResponse.success(recordService.get(userId, meetingId));
    }
}
