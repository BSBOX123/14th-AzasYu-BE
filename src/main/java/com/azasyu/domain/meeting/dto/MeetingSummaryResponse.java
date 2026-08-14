package com.azasyu.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "회의 목록의 항목. 안건과 참여자 상세는 포함하지 않는다.")
public record MeetingSummaryResponse(
    @Schema(description = "회의 식별자", example = "1")
    Long id,

    @Schema(description = "회의 제목", example = "MVP 기능 범위 확정")
    String title,

    @Schema(description = "회의 목적", example = "해커톤에서 구현할 기능 범위를 확정한다.")
    String purpose,

    @Schema(description = "회의 날짜", example = "2026-09-01")
    LocalDate meetingDate,

    @Schema(description = "시작 시각", example = "14:00:00")
    LocalTime startTime,

    @Schema(description = "예상 소요 시간(분)", example = "60")
    Integer expectedDurationMinutes,

    @Schema(description = "참여자 수", example = "3")
    int participantCount
) {
}
