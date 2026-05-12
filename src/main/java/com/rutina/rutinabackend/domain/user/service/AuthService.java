package com.rutina.rutinabackend.domain.user.service;

import com.rutina.rutinabackend.domain.email.service.EmailVerificationService;
import com.rutina.rutinabackend.domain.refreshToken.dto.TokenRefreshRequest;
import com.rutina.rutinabackend.domain.user.dto.*;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.auth.token.RefreshTokenStore;
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
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailVerificationService emailVerificationService;

    // ── 회원가입 ───────────────────────────────────────────
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw ErrorCode.DUPLICATE_EMAIL.toException();
        }

        // 이메일 인증 완료 확인
        emailVerificationService.checkVerified(request.email());

        // 비밀번호 bcrypt 해시
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocal(request.email(), encodedPassword, request.nickname());
        userRepository.save(user);

        emailVerificationService.clearVerified(request.email());

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

        // getRefreshTokenExpiry()는 ms 단위, store 인터페이스는 seconds 단위
        refreshTokenStore.save(user.getId(), device, refreshToken, jwtProvider.getRefreshTokenExpiry() / 1000L);

        return LoginResponse.of(accessToken, refreshToken);
    }

    // ── 토큰 재발급 (refresh token rotation) ────────────────
    @Transactional
    public LoginResponse reissue(TokenRefreshRequest request, String device) {

        // JWT 서명·만료 검증
        if (!jwtProvider.isValid(request.refreshToken())) {
            throw ErrorCode.INVALID_REFRESH_TOKEN.toException();
        }

        // empty → 존재하지 않거나 만료된 토큰 (prod 프로파일은 store 내부에서 DB expiresAt도 검증)
        Long userId = refreshTokenStore.findUserIdByToken(request.refreshToken())
                .orElseThrow(ErrorCode.INVALID_REFRESH_TOKEN::toException);

        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        // rotation: 구 토큰 폐기 후 새 토큰 등록
        refreshTokenStore.deleteByToken(request.refreshToken());
        refreshTokenStore.save(user.getId(), device, newRefreshToken, jwtProvider.getRefreshTokenExpiry() / 1000L);

        return LoginResponse.of(newAccessToken, newRefreshToken);
    }

    // ── 로그아웃 ──────────────────────────────────────────────
    @Transactional // refreshToken으로 기기별 로그아웃
    public void logout(TokenRefreshRequest request) {
        refreshTokenStore.deleteByToken(request.refreshToken());
    }

    // ── 이메일 중복 확인 ─────────────────────────
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}
