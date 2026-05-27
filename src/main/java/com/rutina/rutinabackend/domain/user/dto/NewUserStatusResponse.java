package com.rutina.rutinabackend.domain.user.dto;

import com.rutina.rutinabackend.domain.user.entity.User;

public record NewUserStatusResponse(boolean isNewUser) {
    public static NewUserStatusResponse from(User user) {
        return new NewUserStatusResponse(user.isOnboardingIncomplete());
    }
}
