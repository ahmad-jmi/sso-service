package com.ahmad.sso.service.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record VerifyOtpRequest(
        @NotBlank UUID userId,
        @NotBlank String channel, // "email" or "phone"
        @NotBlank String otp
) {
}
