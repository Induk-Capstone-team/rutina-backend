package com.rutina.rutinabackend.global.oauth2;

import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.auth.oauth2.OAuth2LoginCode;
import com.rutina.rutinabackend.global.auth.oauth2.OAuth2LoginCodeStore;
import com.rutina.rutinabackend.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final long OAUTH2_LOGIN_CODE_TTL_SECONDS = 120;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OAuth2LoginCodeStore oAuth2LoginCodeStore;

    @Value("${oauth2.redirect-url}")
    private String redirectUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = Long.valueOf(String.valueOf(oauth2User.getAttributes().get("userId")));
        boolean isNewUser = Boolean.TRUE.equals(oauth2User.getAttributes().get("isNewUser"));
        String device = request.getHeader("User-Agent");

        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);

        String code = generateLoginCode();
        oAuth2LoginCodeStore.save(
                code,
                new OAuth2LoginCode(user.getId(), isNewUser, device),
                OAUTH2_LOGIN_CODE_TTL_SECONDS
        );

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("code", code)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String generateLoginCode() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
