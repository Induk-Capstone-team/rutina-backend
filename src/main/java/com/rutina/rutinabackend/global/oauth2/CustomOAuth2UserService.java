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
import java.util.Optional;

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
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId());
        boolean isNewUser = existingUser.isEmpty();

        User user;
        if (isNewUser) {
            // 동일 이메일 계정 존재 시 예외 처리
            // 로컬 회원가입과 동일하게 탈퇴 계정의 재가입 제한을 적용합니다.
            userRepository.findByEmailIncludingDeleted(email).ifPresent(existing -> {
                if (existing.getDeletedAt() != null) {
                    throw oauth2Error("탈퇴한 계정은 7일 동안 재가입할 수 없습니다.");
                }
                throw oauth2Error("이미 사용 중인 이메일입니다.");
            });

            if (isBlank(userInfo.getNickname())) {
                throw oauth2Error("소셜 계정의 닉네임 정보가 필요합니다.");
            }
            user = userRepository.save(
                    User.createOAuth(
                            email,
                            userInfo.getNickname(),
                            userInfo.getProvider(),
                            userInfo.getProviderId()
                    )
            );
        } else {
            user = existingUser.get();
        }

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("userId", user.getId());
        attributes.put("isNewUser", isNewUser);

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
            case "google" -> new GoogleUserInfo(attributes);
            default -> throw oauth2Error("Unsupported provider: " + registrationId);
        };
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
