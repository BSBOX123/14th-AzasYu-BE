package com.azasyu.domain.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMeetingRecordRequest(
    @NotBlank @Size(max = 500000) String content
) {
}
