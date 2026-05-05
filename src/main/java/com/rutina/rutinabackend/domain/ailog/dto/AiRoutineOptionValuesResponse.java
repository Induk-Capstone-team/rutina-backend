// AI 루틴 추천 조건 선택지 응답 DTO
package com.rutina.rutinabackend.domain.ailog.dto;

import java.util.List;

public record AiRoutineOptionValuesResponse(
        List<String> purposes,
        List<String> mainActivityTimes,
        List<String> activityTypes,
        List<String> hobbies
) {
}