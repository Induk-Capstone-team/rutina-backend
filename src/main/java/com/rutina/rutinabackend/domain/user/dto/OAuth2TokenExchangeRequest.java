package com.rutina.rutinabackend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuth2TokenExchangeRequest(
        @NotBlank(message = "OAuth2 로그인 코드가 필요합니다.")
        String code
) {
}
