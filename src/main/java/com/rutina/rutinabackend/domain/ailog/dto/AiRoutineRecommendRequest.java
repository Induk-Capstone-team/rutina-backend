// AI 루틴 추천 요청 DTO
package com.rutina.rutinabackend.domain.ailog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AiRoutineRecommendRequest(
        Long categoryId,

        @NotBlank(message = "목적은 필수입니다.")
        String purpose,

        @NotBlank(message = "주요 활동 시간은 필수입니다.")
        String mainActivityTime,

        String activityType,

        @NotEmpty(message = "취미는 최소 1개 이상 선택해야 합니다.")
        List<String> hobbies
) {
}