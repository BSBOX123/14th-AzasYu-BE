package com.azasyu.domain.meeting;

import com.azasyu.domain.meeting.dto.CreateMeetingRequest;
import com.azasyu.domain.meeting.dto.MeetingDetailResponse;
import com.azasyu.domain.meeting.dto.MeetingSummaryResponse;
import com.azasyu.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "회의", description = "회의 생성과 조회. 안건과 참여자는 생성 시 함께 등록한다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(
        summary = "회의 생성",
        description = """
            프로젝트 안에 회의를 만든다. 안건과 참여자를 함께 등록하며,
            **별도의 안건·참여자 관리 API는 없다.**

            생성과 동시에 AI 사전 인터뷰 공통 질문 생성이 시작된다.
            질문 생성 결과는 이 응답에 포함되지 않으므로,
            생성 후 `GET /meetings/{id}/interview/questions`로 상태를 확인한다.

            참여자는 모두 해당 프로젝트의 구성원이어야 한다.
            `meetingDate`는 오늘 이후여야 하며 `expectedDurationMinutes`는 1 이상이어야 한다.

            **성공 시 201 Created**

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 입력 형식이 올바르지 않음 | 400 | `INVALID_REQUEST` |
            | 프로젝트 구성원이 아닌 사용자를 참여자로 지정 | 400 | `INVALID_MEETING_PARTICIPANT` |
            | 프로젝트가 없거나 구성원이 아님 | 404 | `PROJECT_NOT_FOUND` |
            """
    )
    @PostMapping("/projects/{projectId}/meetings")
    ResponseEntity<ApiResponse<MeetingDetailResponse>> create(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long projectId,
        @Valid @RequestBody CreateMeetingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(meetingService.create(userId, projectId, request)));
    }

    @Operation(
        summary = "프로젝트별 회의 목록",
        description = """
            회의 날짜와 시작 시각 기준 **최신순**으로 반환한다.
            목록에는 안건과 참여자 상세가 포함되지 않고 참여자 수만 담긴다.
            상세가 필요하면 회의 상세 조회를 호출한다.

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 프로젝트가 없거나 구성원이 아님 | 404 | `PROJECT_NOT_FOUND` |
            """
    )
    @GetMapping("/projects/{projectId}/meetings")
    ApiResponse<List<MeetingSummaryResponse>> getProjectMeetings(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long projectId
    ) {
        return ApiResponse.success(meetingService.getProjectMeetings(userId, projectId));
    }

    @Operation(
        summary = "회의 상세 조회",
        description = """
            안건, 참여자, 생성자를 포함한 회의 정보를 반환한다.
            **프로젝트 구성원이면 회의 참여자가 아니어도 조회할 수 있다.**

            | 오류 | 상태 | 코드 |
            |---|---|---|
            | 회의를 찾을 수 없음 | 404 | `MEETING_NOT_FOUND` |
            | 회의가 속한 프로젝트의 구성원이 아님 | 404 | `PROJECT_NOT_FOUND` |
            """
    )
    @GetMapping("/meetings/{meetingId}")
    ApiResponse<MeetingDetailResponse> getDetail(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long meetingId
    ) {
        return ApiResponse.success(meetingService.getDetail(userId, meetingId));
    }
}
