package com.azasyu.domain.meeting.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateMeetingRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 1000) String purpose,
    @NotEmpty List<@NotBlank @Size(max = 500) String> agendas,
    @NotNull @FutureOrPresent LocalDate meetingDate,
    @NotNull LocalTime startTime,
    @NotNull @Positive Integer expectedDurationMinutes,
    @NotEmpty List<@NotNull Long> participantUserIds
) {
}
