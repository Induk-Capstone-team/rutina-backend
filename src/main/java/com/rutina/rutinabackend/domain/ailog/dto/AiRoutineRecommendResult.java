// AI 응답 파싱용
package com.rutina.rutinabackend.domain.ailog.dto;
import java.util.List;

public record AiRoutineRecommendResult(
        List<AiRoutineOption> routines
) {
}