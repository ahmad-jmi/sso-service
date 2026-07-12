package com.ahmad.sso.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResetPasswordRequest(
        @NotBlank UUID tenantId,
        @NotBlank String email,
        @NotBlank String otp,
        @NotBlank @Size(min = 8) String newPassword
) {
}
