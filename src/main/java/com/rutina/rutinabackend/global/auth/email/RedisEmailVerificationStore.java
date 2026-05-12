package com.rutina.rutinabackend.global.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class RedisEmailVerificationStore implements EmailVerificationStore {

    // key 패턴: email:{용도}:{email} — Redis TTL이 만료를 자동 관리하므로 조회 시 별도 만료 체크 불필요
    private static final String VERIFY_PREFIX            = "email:verify:";
    private static final String VERIFIED_PREFIX          = "email:verified:";
    private static final String PW_RESET_PREFIX          = "email:pw-reset:";
    private static final String PW_RESET_VERIFIED_PREFIX = "email:pw-reset-verified:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveCode(String email, String code, long ttlSeconds) {
        redisTemplate.opsForValue().set(VERIFY_PREFIX + email, code, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> getCode(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(VERIFY_PREFIX + email));
    }

    @Override
    public void deleteCode(String email) {
        redisTemplate.delete(VERIFY_PREFIX + email);
    }

    @Override
    public void saveVerified(String email, long ttlSeconds) {
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "true", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get(VERIFIED_PREFIX + email));
    }

    @Override
    public void deleteVerified(String email) {
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }

    @Override
    public void savePwResetCode(String email, String code, long ttlSeconds) {
        redisTemplate.opsForValue().set(PW_RESET_PREFIX + email, code, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> getPwResetCode(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PW_RESET_PREFIX + email));
    }

    @Override
    public void deletePwResetCode(String email) {
        redisTemplate.delete(PW_RESET_PREFIX + email);
    }

    @Override
    public void savePwResetVerified(String email, long ttlSeconds) {
        redisTemplate.opsForValue().set(PW_RESET_VERIFIED_PREFIX + email, "true", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isPwResetVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get(PW_RESET_VERIFIED_PREFIX + email));
    }

    @Override
    public void deletePwResetVerified(String email) {
        redisTemplate.delete(PW_RESET_VERIFIED_PREFIX + email);
    }
}
