package com.ahmad.sso.service.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record LoginRequest(
        @NotBlank UUID tenantId,
        @NotBlank String email,
        @NotBlank String password
) {
}
