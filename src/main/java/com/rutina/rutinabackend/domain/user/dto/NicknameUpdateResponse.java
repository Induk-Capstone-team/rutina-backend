package com.rutina.rutinabackend.domain.user.dto;

import com.rutina.rutinabackend.domain.user.entity.User;

// 닉네임 변경 후 클라이언트에 내려줄 응답 DTO입니다.
public record NicknameUpdateResponse(
        Long userId,
        String nickname
) {
    public static NicknameUpdateResponse from(User user) {
        // 엔티티 전체를 그대로 노출하지 않고 필요한 값만 응답합니다.
        return new NicknameUpdateResponse(user.getId(), user.getNickname());
    }
}
