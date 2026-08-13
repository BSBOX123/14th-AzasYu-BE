package com.azasyu.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JoinProjectRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9]{8}") String joinCode
) {
}
