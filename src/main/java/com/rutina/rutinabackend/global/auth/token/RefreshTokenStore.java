package com.rutina.rutinabackend.global.auth.token;

import java.util.Optional;

public interface RefreshTokenStore {
    void save(Long userId, String device, String token, long ttlSeconds);
    Optional<Long> findUserIdByToken(String token);
    void deleteByUserIdAndDevice(Long userId, String device);
    void deleteByToken(String token);
    void deleteAllByUserId(Long userId);
}
