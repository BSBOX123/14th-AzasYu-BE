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
    int participantCount,

    @Schema(description = """
        로그인한 사용자가 이 회의의 참여자인지 여부.

        목록에는 프로젝트의 모든 회의가 나오므로, 내가 참여하지 않는 회의도 포함된다.
        `false`인 회의는 상세 조회는 되지만 사전 인터뷰·원문 등록·분석 결과 조회가 막힌다.
        참여하려면 회의 참여 코드로 합류해야 한다.
        """, example = "true")
    boolean participating
) {
}
