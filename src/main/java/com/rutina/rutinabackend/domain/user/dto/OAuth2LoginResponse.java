package com.rutina.rutinabackend.domain.user.dto;

public record OAuth2LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        boolean isNewUser
) {
    public static OAuth2LoginResponse of(String accessToken, String refreshToken, boolean isNewUser) {
        return new OAuth2LoginResponse(accessToken, refreshToken, "Bearer", isNewUser);
    }
}
