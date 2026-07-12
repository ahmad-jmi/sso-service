package com.ahmad.sso.service.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ForgotPasswordRequest(
        @NotBlank UUID tenantId,
        @NotBlank String email
) {
}
