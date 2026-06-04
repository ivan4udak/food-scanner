package com.foodscanner.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminResetPasswordRequest(
    @NotBlank(message = "Role must not be blank")     String role,
    @NotBlank(message = "Password must not be blank") String password,
    @NotBlank(message = "Username must not be blank") String username
) {}
