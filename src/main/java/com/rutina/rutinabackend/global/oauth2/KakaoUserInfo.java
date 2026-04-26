package com.rutina.rutinabackend.global.oauth2;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = kakaoAccount == null
                ? Map.of()
                : (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());
    }

    @Override
    public String getProvider() {
        return "KAKAO";
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id == null ? null : String.valueOf(id);
    }

    @Override
    public String getEmail() {
        Object email = kakaoAccount == null ? null : kakaoAccount.get("email");
        return email == null ? null : String.valueOf(email);
    }

    @Override
    public String getNickname() {
        Object nickname = profile.get("nickname");
        return nickname == null ? null : String.valueOf(nickname);
    }
}
