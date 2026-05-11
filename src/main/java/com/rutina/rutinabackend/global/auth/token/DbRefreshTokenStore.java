package com.rutina.rutinabackend.global.auth.token;

import com.rutina.rutinabackend.domain.refreshToken.entity.RefreshToken;
import com.rutina.rutinabackend.domain.refreshToken.repository.RefreshTokenRepository;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Profile("db")
@Component
@RequiredArgsConstructor
public class DbRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public void save(Long userId, String device, String token, long ttlSeconds) {
        User user = userRepository.findById(userId).orElseThrow();
        refreshTokenRepository.deleteByUserIdAndDevice(userId, device);
        // 인터페이스 계약은 seconds, RefreshToken.of()는 ms를 받으므로 *1000L 변환
        refreshTokenRepository.save(RefreshToken.of(user, token, ttlSeconds * 1000L, device));
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        // JWT 서명 검증(호출자)과 별도로 DB expiresAt을 이중 검증 — 강제 폐기 시나리오 대응
        return refreshTokenRepository.findByTokenValue(token)
                .filter(rt -> rt.getExpiresAt().isAfter(OffsetDateTime.now()))
                .map(rt -> rt.getUser().getId());
    }

    @Override
    public void deleteByUserIdAndDevice(Long userId, String device) {
        refreshTokenRepository.deleteByUserIdAndDevice(userId, device);
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepository.findByTokenValue(token)
                .ifPresent(refreshTokenRepository::delete);
    }
}
