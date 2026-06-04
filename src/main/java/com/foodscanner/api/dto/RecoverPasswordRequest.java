package com.foodscanner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoverPasswordRequest(
    @NotBlank(message = "Username must not be blank") String username,
    @NotBlank(message = "Password must not be blank")
    @Size(min = 4, max = 100, message = "Password must be 4..100 chars") String password
) {}
