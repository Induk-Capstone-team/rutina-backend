package com.rutina.rutinabackend.domain.ailog.dto;

public record AiRequest(
        Long userId,
        String question
) {
}