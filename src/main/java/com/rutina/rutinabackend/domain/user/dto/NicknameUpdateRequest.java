package com.rutina.rutinabackend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 닉네임 변경 요청에서 필요한 값만 받기 위한 DTO입니다.
public record NicknameUpdateRequest(
        // 공백 닉네임은 허용하지 않고, 길이를 2~10자로 제한합니다.
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자 사이여야 합니다.")
        String nickname
) {}