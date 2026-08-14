package com.azasyu.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "회의 상세. 안건과 참여자를 포함한다.")
public record MeetingDetailResponse(
    @Schema(description = "회의 식별자", example = "1")
    Long id,

    @Schema(description = "회의가 속한 프로젝트 식별자", example = "1")
    Long projectId,

    @Schema(description = "회의 제목", example = "MVP 기능 범위 확정")
    String title,

    @Schema(description = "회의 목적", example = "해커톤에서 구현할 기능 범위를 확정한다.")
    String purpose,

    @Schema(description = "안건 목록. order 오름차순으로 정렬된다.")
    List<AgendaResponse> agendas,

    @Schema(description = "회의 날짜", example = "2026-09-01")
    LocalDate meetingDate,

    @Schema(description = "시작 시각", example = "14:00:00")
    LocalTime startTime,

    @Schema(description = "예상 소요 시간(분)", example = "60")
    Integer expectedDurationMinutes,

    @Schema(description = "회의를 만든 사람")
    CreatorResponse creator,

    @Schema(description = "참여자 목록")
    List<ParticipantResponse> participants,

    @Schema(description = "회의 생성 시각", example = "2026-08-14T10:00:00")
    LocalDateTime createdAt
) {
    @Schema(description = "회의 안건")
    public record AgendaResponse(
        @Schema(description = "안건 식별자", example = "1")
        Long id,

        @Schema(description = "안건 순번. 1부터 시작한다.", example = "1")
        int order,

        @Schema(description = "안건 내용", example = "핵심 기능 범위")
        String content
    ) {
    }

    @Schema(description = "회의 생성자")
    public record CreatorResponse(
        @Schema(description = "사용자 식별자", example = "1")
        Long userId,

        @Schema(description = "표시 이름", example = "홍길동")
        String name
    ) {
    }

    @Schema(description = "회의 참여자")
    public record ParticipantResponse(
        @Schema(description = "사용자 식별자", example = "2")
        Long userId,

        @Schema(description = "표시 이름", example = "김영희")
        String name
    ) {
    }
}
