package com.rutina.rutinabackend.global.oauth2;

public interface OAuth2UserInfo {
    String getProvider();
    String getProviderId();
    String getEmail();
    String getNickname();
}

