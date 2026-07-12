package com.ahmad.sso.service.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Tracks revoked access-token IDs (jti) so a logged-out/revoked JWT stops working
// immediately, even though the JWT itself remains cryptographically valid until expiry.
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "blacklist:jti:";

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String jti, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
