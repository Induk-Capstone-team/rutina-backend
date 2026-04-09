package com.rutina.rutinabackend.domain.user.service;

import com.rutina.rutinabackend.domain.user.dto.*;
import com.rutina.rutinabackend.domain.refreshToken.entity.RefreshToken;
import com.rutina.rutinabackend.domain.user.entity.User;
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
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(ErrorCode.INVALID_CREDENTIALS::toException);

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ErrorCode.INVALID_CREDENTIALS.toException();
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        // 기존 refresh token 삭제 후 신규 저장 (1인 1토큰)
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(
                RefreshToken.of(user, refreshToken, jwtProvider.getRefreshTokenExpiry())
        );

        return LoginResponse.of(accessToken, refreshToken);
    }

    // ── 이메일 중복 확인 ─────────────────────────
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}
