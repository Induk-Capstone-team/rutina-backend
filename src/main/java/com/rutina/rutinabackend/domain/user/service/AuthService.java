package com.rutina.rutinabackend.domain.user.service;

import com.rutina.rutinabackend.domain.user.dto.*;
import com.rutina.rutinabackend.domain.refreshToken.entity.RefreshToken;
import com.rutina.rutinabackend.domain.user.entity.User;
import java.time.OffsetDateTime;
import com.rutina.rutinabackend.domain.refreshToken.repository.RefreshTokenRepository;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.exception.ErrorCode;
import com.rutina.rutinabackend.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ── 회원가입 ───────────────────────────────────────────
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw ErrorCode.DUPLICATE_EMAIL.toException();
        }

        // 비밀번호 bcrypt 해시
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocal(request.email(), encodedPassword, request.nickname());
        userRepository.save(user);

        return SignUpResponse.from(user);
    }

    // ── 로그인 ─────────────────────────────────────────────
    @Transactional
    public LoginResponse login(LoginRequest request, String device) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(ErrorCode.INVALID_CREDENTIALS::toException);

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ErrorCode.INVALID_CREDENTIALS.toException();
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        // 같은 기기 재로그인 시 해당 기기 토큰만 교체 (다른 기기 토큰 유지)
        refreshTokenRepository.deleteByUserIdAndDevice(user.getId(), device);
        refreshTokenRepository.save(
                RefreshToken.of(user, refreshToken, jwtProvider.getRefreshTokenExpiry(), device)
        );

        return LoginResponse.of(accessToken, refreshToken);
    }

    // ── 토큰 재발급 (refresh token rotation) ────────────────
    @Transactional
    public LoginResponse reissue(String refreshToken, String device) {

        // JWT 서명·만료 검증
        if (!jwtProvider.isValid(refreshToken)) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException();
        }

        // DB에 저장된 토큰인지 확인
        RefreshToken saved = refreshTokenRepository.findByTokenValue(refreshToken)
                .orElseThrow(ErrorCode.INVALID_REFRESH_TOKEN::toException);

        // DB expires_at 만료 검증
        if (saved.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(saved);
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException();
        }

        User user = saved.getUser();

        // 새 토큰 발급 (rotation)
        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        // 기존 토큰 교체
        refreshTokenRepository.delete(saved);
        refreshTokenRepository.save(
                RefreshToken.of(user, newRefreshToken, jwtProvider.getRefreshTokenExpiry(), device)
        );

        return LoginResponse.of(newAccessToken, newRefreshToken);
    }

    // ── 로그아웃 ──────────────────────────────────────────────
    @Transactional // refreshToken으로 기기별 로그아웃
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenValue(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    // ── 이메일 중복 확인 ─────────────────────────
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}
