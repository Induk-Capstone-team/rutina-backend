// AI 루틴 추천 응답 DTO
package com.rutina.rutinabackend.domain.ailog.dto;
import java.util.List;

public record AiRoutineRecommendResponse(
        Long recommendationId,
        Long categoryId,
        List<AiRoutineOption> routines
) {
}