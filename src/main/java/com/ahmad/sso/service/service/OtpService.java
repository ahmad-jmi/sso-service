package com.ahmad.sso.service.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

// Stores OTPs in Redis with a TTL rather than Postgres - they're inherently
// short-lived and don't need durability. Swap in a real email/SMS provider
// (SES/SNS or Twilio) in place of the log statements below.
@Service
public class OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;

    public OtpService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateAndSend(UUID userId, String channel, String destination) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = otpKey(userId, channel);
        redisTemplate.opsForValue().set(key, otp, OTP_TTL);

        // TODO: replace with real SES/Twilio integration
        System.out.printf("[OTP] Sending %s OTP to %s: %s%n", channel, destination, otp);

        return otp; // returned here only for local testing/demo purposes - do not return in real API responses
    }

    public boolean verify(UUID userId, String channel, String submittedOtp) {
        String key = otpKey(userId, channel);
        String stored = redisTemplate.opsForValue().get(key);
        boolean matches = stored != null && stored.equals(submittedOtp);
        if (matches) {
            redisTemplate.delete(key);
        }
        return matches;
    }

    private String otpKey(UUID userId, String channel) {
        return "otp:" + channel + ":" + userId;
    }
}
