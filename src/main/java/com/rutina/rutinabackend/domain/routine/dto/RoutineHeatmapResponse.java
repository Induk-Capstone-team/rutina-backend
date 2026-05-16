package com.rutina.rutinabackend.domain.routine.dto;

import com.rutina.rutinabackend.domain.routine.entity.Routine;

import java.util.Map;

public record RoutineHeatmapResponse(
        Long routineId,
        String title,
        RoutineHeatmapCategoryResponse category,
        Map<String, Boolean> completed
) {
    // completed에는 완료된 날짜 키만 true로 포함
    public static RoutineHeatmapResponse from(Routine routine, Map<String, Boolean> completed) {
        return new RoutineHeatmapResponse(
                routine.getId(),
                routine.getTitle(),
                RoutineHeatmapCategoryResponse.from(routine.getCategory()),
                completed
        );
    }
}
