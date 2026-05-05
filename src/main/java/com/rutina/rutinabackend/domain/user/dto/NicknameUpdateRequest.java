package com.rutina.rutinabackend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 닉네임 변경 요청에서 필요한 값만 받기 위한 DTO입니다.
public record NicknameUpdateRequest(
        // 공백 닉네임은 허용하지 않고, 길이를 30자로 제한합니다.
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 30, message = "닉네임은 30자 이하로 입력해주세요.")
        String nickname
) {
}
