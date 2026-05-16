package com.rutina.rutinabackend.global.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX  = "refresh:token:";
    private static final String DEVICE_KEY_PREFIX = "refresh:device:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long userId, String device, String token, long ttlSeconds) {
        String deviceKey = deviceKey(userId, device);

        // 같은 기기의 기존 token이 있으면 token 역조회 키도 함께 삭제
        String existingToken = redisTemplate.opsForValue().get(deviceKey);
        if (existingToken != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + existingToken);
        }

        redisTemplate.opsForValue().set(deviceKey, token, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, String.valueOf(userId), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        // Redis TTL이 만료 검증을 대신하므로 별도 검증 불필요
        String value = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        return Optional.ofNullable(value).map(Long::valueOf);
    }

    @Override
    public void deleteByUserIdAndDevice(Long userId, String device) {
        String deviceKey = deviceKey(userId, device);
        String token = redisTemplate.opsForValue().get(deviceKey);
        if (token != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        }
        redisTemplate.delete(deviceKey);
    }

    @Override
    public void deleteByToken(String token) {
        String userIdStr = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        if (userIdStr != null) {
            // device를 모르므로 scan으로 해당 userId의 device 키 중 값이 token과 일치하는 것을 찾아 삭제
            String pattern = DEVICE_KEY_PREFIX + userIdStr + ":*";
            Set<String> deviceKeys = redisTemplate.keys(pattern);
            if (deviceKeys != null) {
                deviceKeys.stream()
                        .filter(k -> token.equals(redisTemplate.opsForValue().get(k)))
                        .forEach(redisTemplate::delete);
            }
        }
        redisTemplate.delete(TOKEN_KEY_PREFIX + token);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        String pattern = DEVICE_KEY_PREFIX + userId + ":*";
        Set<String> deviceKeys = redisTemplate.keys(pattern);
        if (deviceKeys != null) {
            deviceKeys.forEach(deviceKey -> {
                String token = redisTemplate.opsForValue().get(deviceKey);
                if (token != null) {
                    redisTemplate.delete(TOKEN_KEY_PREFIX + token);
                }
                redisTemplate.delete(deviceKey);
            });
        }
    }

    private String deviceKey(Long userId, String device) {
        return DEVICE_KEY_PREFIX + userId + ":" + device;
    }
}
