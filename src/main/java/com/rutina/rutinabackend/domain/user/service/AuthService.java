package com.rutina.rutinabackend.domain.user.service;

import com.rutina.rutinabackend.domain.email.service.EmailVerificationService;
import com.rutina.rutinabackend.domain.refreshToken.dto.TokenRefreshRequest;
import com.rutina.rutinabackend.domain.user.dto.*;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.auth.apple.AppleIdentityTokenVerifier;
import com.rutina.rutinabackend.global.auth.oauth2.OAuth2LoginCode;
import com.rutina.rutinabackend.global.auth.oauth2.OAuth2LoginCodeStore;
import com.rutina.rutinabackend.global.auth.token.RefreshTokenStore;
import com.rutina.rutinabackend.global.exception.BusinessException;
import com.rutina.rutinabackend.global.exception.ErrorCode;
import com.rutina.rutinabackend.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailVerificationService emailVerificationService;
    private final AppleIdentityTokenVerifier appleIdentityTokenVerifier;
    private final OAuth2LoginCodeStore oAuth2LoginCodeStore;

    // ── 회원가입 ───────────────────────────────────────────
    @Transactional
    public LoginResponse signUp(SignUpRequest request, String device) {

        // 탈퇴 후 7일 보관 중인 계정은 조회하지 못함.
        userRepository.findByEmailIncludingDeleted(request.email()).ifPresent(existing -> {
            if (existing.getDeletedAt() != null) {
                // 탈퇴한 계정 — 7일 보관 중
                throw ErrorCode.WITHDRAWN_USER.toException();
            }
            throw ErrorCode.DUPLICATE_EMAIL.toException();
        });

        // 이메일 인증 완료 확인
        emailVerificationService.checkVerified(request.email());

        // 비밀번호 bcrypt 해시
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocal(request.email(), encodedPassword, request.nickname());
        userRepository.save(user);

        // 이메일 인증 삭제
        emailVerificationService.clearVerified(request.email());

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        refreshTokenStore.save(user.getId(), device, refreshToken, jwtProvider.getRefreshTokenExpiry() / 1000L);

        return LoginResponse.of(accessToken, refreshToken);
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

    @Transactional
    public OAuth2LoginResponse exchangeOAuth2Code(OAuth2TokenExchangeRequest request, String device) {
        OAuth2LoginCode loginCode = oAuth2LoginCodeStore.consume(request.code())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_OAUTH2_LOGIN_CODE",
                        "유효하지 않거나 만료된 소셜 로그인 코드입니다."
                ));

        User user = userRepository.findById(loginCode.userId())
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());
        String tokenDevice = StringUtils.hasText(device) ? device : loginCode.device();

        refreshTokenStore.save(user.getId(), tokenDevice, refreshToken, jwtProvider.getRefreshTokenExpiry() / 1000L);

        return OAuth2LoginResponse.of(accessToken, refreshToken, loginCode.isNewUser());
    }

    @Transactional
    public LoginResponse loginWithApple(AppleLoginRequest request, String device) {
        // Apple identityToken의 서명, 발급자, 대상 앱(Bundle ID)을 검증합니다.
        Jwt appleToken = appleIdentityTokenVerifier.verify(request.identityToken());
        String providerId = appleToken.getSubject();

        // Apple의 고유 사용자 식별자(sub)를 providerId로 사용해 기존 계정을 찾습니다.
        User user = userRepository.findByProviderAndProviderId("APPLE", providerId)
                .orElseGet(() -> createAppleUser(request, appleToken, providerId));

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());

        refreshTokenStore.save(user.getId(), device, refreshToken, jwtProvider.getRefreshTokenExpiry() / 1000L);

        return LoginResponse.of(accessToken, refreshToken);
    }

    private User createAppleUser(AppleLoginRequest request, Jwt appleToken, String providerId) {
        String email = resolveAppleEmail(request, appleToken, providerId);

        // 탈퇴 보관 계정과 기존 이메일 계정은 Apple 신규 가입으로 덮어쓰지 않습니다.
        userRepository.findByEmailIncludingDeleted(email).ifPresent(existing -> {
            if (existing.getDeletedAt() != null) {
                throw ErrorCode.WITHDRAWN_USER.toException();
            }
            throw ErrorCode.SOCIAL_ACCOUNT_CONFLICT.toException();
        });

        String nickname = resolveAppleNickname(request, email);
        User user = User.createOAuth(email, nickname, "APPLE", providerId);
        return userRepository.save(user);
    }

    private String resolveAppleEmail(AppleLoginRequest request, Jwt appleToken, String providerId) {
        // Apple email/name은 최초 로그인 때만 내려올 수 있어 앱에서 전달한 값을 우선 사용합니다.
        if (StringUtils.hasText(request.email())) {
            return request.email();
        }

        String tokenEmail = appleToken.getClaimAsString("email");
        if (StringUtils.hasText(tokenEmail)) {
            return tokenEmail;
        }

        return "apple_" + providerId + "@social.local";
    }

    private String resolveAppleNickname(AppleLoginRequest request, String email) {
        // 이름을 받지 못한 경우 이메일 앞부분을 기본 닉네임으로 사용합니다.
        if (StringUtils.hasText(request.nickname())) {
            return request.nickname();
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return "Apple User";
    }
}
