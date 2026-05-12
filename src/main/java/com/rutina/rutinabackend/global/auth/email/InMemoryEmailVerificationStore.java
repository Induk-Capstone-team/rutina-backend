package com.rutina.rutinabackend.global.auth.email;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Profile("local")
@Component
public class InMemoryEmailVerificationStore implements EmailVerificationStore {

    private final Map<String, String> values   = new ConcurrentHashMap<>();
    private final Map<String, Long>   expiries = new ConcurrentHashMap<>();

    @Override
    public void saveCode(String email, String code, long ttlSeconds) {
        put("verify:" + email, code, ttlSeconds);
    }

    @Override
    public Optional<String> getCode(String email) {
        return get("verify:" + email);
    }

    @Override
    public void deleteCode(String email) {
        delete("verify:" + email);
    }

    @Override
    public void saveVerified(String email, long ttlSeconds) {
        put("verified:" + email, "true", ttlSeconds);
    }

    @Override
    public boolean isVerified(String email) {
        return get("verified:" + email).isPresent();
    }

    @Override
    public void deleteVerified(String email) {
        delete("verified:" + email);
    }

    @Override
    public void savePwResetCode(String email, String code, long ttlSeconds) {
        put("pw-reset:" + email, code, ttlSeconds);
    }

    @Override
    public Optional<String> getPwResetCode(String email) {
        return get("pw-reset:" + email);
    }

    @Override
    public void deletePwResetCode(String email) {
        delete("pw-reset:" + email);
    }

    @Override
    public void savePwResetVerified(String email, long ttlSeconds) {
        put("pw-reset-verified:" + email, "true", ttlSeconds);
    }

    @Override
    public boolean isPwResetVerified(String email) {
        return get("pw-reset-verified:" + email).isPresent();
    }

    @Override
    public void deletePwResetVerified(String email) {
        delete("pw-reset-verified:" + email);
    }

    private void put(String key, String value, long ttlSeconds) {
        values.put(key, value);
        // ttlSeconds를 절대 만료 시각(ms)으로 변환해 저장
        expiries.put(key, System.currentTimeMillis() + ttlSeconds * 1000);
    }

    private Optional<String> get(String key) {
        Long expiry = expiries.get(key);
        // expiry가 null이면 키 자체가 없는 것, 현재 시각이 만료 시각을 초과하면 만료 → 두 맵 모두 정리
        if (expiry == null || System.currentTimeMillis() > expiry) {
            values.remove(key);
            expiries.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(key));
    }

    private void delete(String key) {
        values.remove(key);
        expiries.remove(key);
    }
}
