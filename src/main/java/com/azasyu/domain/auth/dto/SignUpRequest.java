package com.azasyu.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(min = 8, max = 72) String password
) {
}
