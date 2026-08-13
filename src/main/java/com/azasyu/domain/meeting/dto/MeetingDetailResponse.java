package com.azasyu.domain.meeting.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MeetingDetailResponse(
    Long id,
    Long projectId,
    String title,
    String purpose,
    List<AgendaResponse> agendas,
    LocalDate meetingDate,
    LocalTime startTime,
    Integer expectedDurationMinutes,
    CreatorResponse creator,
    List<ParticipantResponse> participants,
    LocalDateTime createdAt
) {
    public record AgendaResponse(Long id, int order, String content) {
    }

    public record CreatorResponse(Long userId, String name) {
    }

    public record ParticipantResponse(Long userId, String name) {
    }
}
