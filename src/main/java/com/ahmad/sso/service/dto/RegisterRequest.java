package com.ahmad.sso.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank UUID tenantId,
        @NotBlank @Email String email,
        String phone,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {
}
