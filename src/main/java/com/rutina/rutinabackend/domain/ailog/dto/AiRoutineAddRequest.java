// AI 추천 루틴 추가 요청 DTO
package com.rutina.rutinabackend.domain.ailog.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AiRoutineAddRequest(
        Long categoryId,
// 추천 요청 단계 또는 추가 요청 단계 중 한 곳에서만 받아오면 됨.

        @NotEmpty(message = "추가할 루틴을 최소 1개 이상 선택해야 합니다.")
        List<Integer> selectedOptionIds
) {
}