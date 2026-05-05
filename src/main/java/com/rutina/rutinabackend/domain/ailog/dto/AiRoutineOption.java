// AI가 추천하는 후보 1개
package com.rutina.rutinabackend.domain.ailog.dto;

public record AiRoutineOption(
        Integer optionId,
        String title,
        String recommendedTime,
        Integer durationMinutes
) {
}