package com.rutina.rutinabackend.global.auth.oauth2;

public record OAuth2LoginCode(
        Long userId,
        boolean isNewUser,
        String device
) {
}
