package com.rutina.rutinabackend.domain.user.dto;

import com.rutina.rutinabackend.domain.user.entity.User;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String provider,
        Integer gender,
        String age,
        String job,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getGender(),
                user.getAge(),
                user.getJob(),
                user.getCreatedAt()
        );
    }
}
