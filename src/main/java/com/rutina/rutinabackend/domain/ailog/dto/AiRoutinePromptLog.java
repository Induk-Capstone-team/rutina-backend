// AiLog.prompt에 저장할 추천 요청 정보

package com.rutina.rutinabackend.domain.ailog.dto;

import java.util.List;

public record AiRoutinePromptLog(
        Long userId,
        String job,
        String gender,
        String age,
        Long categoryId,
        String categoryName,
        String purpose,
        String mainActivityTime,
        String activityType,
        List<String> hobbies
) {
}