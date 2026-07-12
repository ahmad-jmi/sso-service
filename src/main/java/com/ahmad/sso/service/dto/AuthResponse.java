package com.ahmad.sso.service.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
