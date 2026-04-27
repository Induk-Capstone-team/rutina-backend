package com.rutina.rutinabackend.domain.routine.dto;

import com.rutina.rutinabackend.domain.routine.entity.Routine;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

// 타임테이블 화면용 응답 DTO
@Getter
@Builder
public class RoutineTimetableResponse {

    private Long routineId;
    private Long categoryId;
    private String categoryColorCode;
    private String title;
    private LocalTime startTime;
    private LocalTime endTime;

    // 엔티티 → 타임테이블 응답 DTO 변환 정적 팩토리
    public static RoutineTimetableResponse from(Routine routine) {
        return RoutineTimetableResponse.builder()
                .routineId(routine.getId())
                .categoryId(routine.getCategory().getId())
                .categoryColorCode(routine.getCategory().getColorCode())
                .title(routine.getTitle())
                .startTime(routine.getStartTime())
                .endTime(routine.getEndTime())
                .build();
    }
}
