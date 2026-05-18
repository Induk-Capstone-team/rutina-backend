package com.rutina.rutinabackend.global.auth.apple;

import com.rutina.rutinabackend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class AppleIdentityTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_JWK_SET_URI = "https://appleid.apple.com/auth/keys";

    private final JwtDecoder jwtDecoder;
    private final String bundleId;

    public AppleIdentityTokenVerifier(@Value("${apple.bundle-id:}") String bundleId) {
        this.bundleId = bundleId;

        // Apple 공개키로 identityToken 서명을 검증하고 issuer/audience 조건을 함께 확인합니다.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud",
                audiences -> StringUtils.hasText(bundleId) && audiences != null && audiences.contains(bundleId)
        );
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(APPLE_ISSUER),
                audienceValidator
        );

        decoder.setJwtValidator(jwt -> {
            OAuth2TokenValidatorResult result = validator.validate(jwt);
            if (result.hasErrors()) {
                return result;
            }

            if (!StringUtils.hasText(jwt.getSubject())) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Apple identity token subject is missing", null)
                );
            }

            return OAuth2TokenValidatorResult.success();
        });
        this.jwtDecoder = decoder;
    }

    public Jwt verify(String identityToken) {
        // Bundle ID가 설정되지 않은 환경에서는 Apple 토큰을 신뢰하지 않습니다.
        if (!StringUtils.hasText(bundleId)) {
            throw ErrorCode.INVALID_APPLE_IDENTITY_TOKEN.toException();
        }

        try {
            return jwtDecoder.decode(identityToken);
        } catch (JwtException e) {
            throw ErrorCode.INVALID_APPLE_IDENTITY_TOKEN.toException();
        }
    }
}
