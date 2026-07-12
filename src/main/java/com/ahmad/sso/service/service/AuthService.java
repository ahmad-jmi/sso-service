package com.ahmad.sso.service.service;

import com.ahmad.sso.service.dto.*;
import com.ahmad.sso.service.entity.RefreshToken;
import com.ahmad.sso.service.entity.User;
import com.ahmad.sso.service.entity.UserStatus;
import com.ahmad.sso.service.exception.ApiException;
import com.ahmad.sso.service.repository.RefreshTokenRepository;
import com.ahmad.sso.service.repository.UserRepository;
import com.ahmad.sso.service.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserEventPublisher userEventPublisher;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            UserEventPublisher userEventPublisher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userEventPublisher = userEventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByTenantIdAndEmail(request.tenantId(), request.email())) {
            throw new ApiException("Email already registered for this tenant", 409);
        }

        User user = User.builder()
                .tenantId(request.tenantId())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
//        userEventPublisher.publishUserCreated(user.getId(), user.getTenantId(), user.getEmail());

        return issueTokens(user, List.of(), List.of());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByTenantIdAndEmail(request.tenantId(), request.email())
                .orElseThrow(() -> new ApiException("Invalid credentials", 401));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException("Invalid credentials", 401);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException("Account is not active", 403);
        }

        // TODO: fetch actual roles/permissions from RoleRepository/UserRoleRepository
        // and include them as JWT claims for downstream RBAC checks.
        return issueTokens(user, List.of(), List.of());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hash(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException("Invalid refresh token", 401));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Refresh token expired or revoked", 401);
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException("User not found", 401));

        // Rotate: revoke the old refresh token, issue a new pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(user, List.of(), List.of());
    }

    @Transactional
    public void logout(UUID userId, String accessTokenJti, long remainingTtlSeconds) {
        // Blacklist the current access token so it stops working immediately
        if (remainingTtlSeconds > 0) {
            tokenBlacklistService.blacklist(accessTokenJti, Duration.ofSeconds(remainingTtlSeconds));
        }
        // Revoke all refresh tokens - full "logout everywhere"
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    private AuthResponse issueTokens(User user, List<String> roles, List<String> permissions) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getTenantId(), roles, permissions);

        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
