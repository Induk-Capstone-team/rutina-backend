package com.rutina.rutinabackend.domain.routine.dto;

import com.rutina.rutinabackend.domain.category.entity.Category;

public record RoutineHeatmapCategoryResponse(
        Long id,
        String name,
        String colorCode
) {
    // 카테고리가 없는 루틴은 null로 응답
    public static RoutineHeatmapCategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }

        return new RoutineHeatmapCategoryResponse(
                category.getId(),
                category.getName(),
                category.getColorCode()
        );
    }
}
