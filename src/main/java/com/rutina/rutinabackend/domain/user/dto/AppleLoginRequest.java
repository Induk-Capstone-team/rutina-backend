package com.rutina.rutinabackend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(

        @NotBlank(message = "Apple identity token은 필수입니다.")
        String identityToken,

        String email,

        String nickname
) {
}
