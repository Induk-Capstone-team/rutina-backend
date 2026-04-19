package com.rutina.rutinabackend.global.oauth2;

import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        if (isBlank(userInfo.getProviderId())) {
            throw oauth2Error("OAuth2 account information is incomplete");
        }
        // 이메일 비제공 계정은 가상 이메일 생성 (카카오는 이메일 미동의 가능)
        String email = isBlank(userInfo.getEmail())
                ? userInfo.getProvider().toLowerCase() + "_" + userInfo.getProviderId() + "@social.local"
                : userInfo.getEmail();

        // provider + providerId 기준으로 기존 회원 조회, 없으면 자동 가입
        User user = userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .orElseGet(() -> userRepository.save(
                        User.createOAuth(
                                email,
                                defaultNickname(userInfo, email),
                                userInfo.getProvider(),
                                userInfo.getProviderId()
                        )
                ));

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("userId", user.getId());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                attributes,
                "userId"
        );
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw oauth2Error("Unsupported provider: " + registrationId);
        };
    }

    private String defaultNickname(OAuth2UserInfo userInfo, String email) {
        if (!isBlank(userInfo.getNickname())) {
            return userInfo.getNickname();
        }
        return email.split("@")[0];
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private OAuth2AuthenticationException oauth2Error(String message) {
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_oauth2_user"),
                message
        );
    }
}
