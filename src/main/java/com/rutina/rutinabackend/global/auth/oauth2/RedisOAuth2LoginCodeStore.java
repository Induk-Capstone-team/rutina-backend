package com.rutina.rutinabackend.global.auth.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class RedisOAuth2LoginCodeStore implements OAuth2LoginCodeStore {

    private static final String CODE_KEY_PREFIX = "oauth2:login-code:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String code, OAuth2LoginCode loginCode, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    key(code),
                    objectMapper.writeValueAsString(loginCode),
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OAuth2 로그인 코드 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<OAuth2LoginCode> consume(String code) {
        String key = key(code);
        String value = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, OAuth2LoginCode.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private String key(String code) {
        return CODE_KEY_PREFIX + code;
    }
}
