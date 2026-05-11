package com.rutina.rutinabackend.global.auth.token;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Profile("local")
@Component
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    // token → userId (토큰값으로 userId 역조회)
    private final Map<String, Long> tokenToUser = new ConcurrentHashMap<>();
    // "userId:device" → token (기기별 단일 토큰 보장, 중복 로그인 시 이전 토큰 무효화용)
    private final Map<String, String> userDeviceToToken = new ConcurrentHashMap<>();

    @Override
    public void save(Long userId, String device, String token, long ttlSeconds) {
        String key = userId + ":" + device;
        String existingToken = userDeviceToToken.get(key);
        // 같은 기기로 재로그인하면 이전 토큰이 tokenToUser에 남아 역조회되는 것을 방지
        if (existingToken != null) {
            tokenToUser.remove(existingToken);
        }
        userDeviceToToken.put(key, token);
        tokenToUser.put(token, userId);
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        return Optional.ofNullable(tokenToUser.get(token));
    }

    @Override
    public void deleteByUserIdAndDevice(Long userId, String device) {
        String key = userId + ":" + device;
        String token = userDeviceToToken.remove(key);
        if (token != null) {
            tokenToUser.remove(token);
        }
    }

    @Override
    public void deleteByToken(String token) {
        Long userId = tokenToUser.remove(token);
        if (userId != null) {
            // userDeviceToToken에 token → key 역방향 인덱스가 없으므로 value 스캔으로 제거
            userDeviceToToken.entrySet().removeIf(e -> e.getValue().equals(token));
        }
    }
}
