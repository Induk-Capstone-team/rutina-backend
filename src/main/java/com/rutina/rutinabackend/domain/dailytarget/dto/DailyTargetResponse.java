package com.rutina.rutinabackend.domain.dailytarget.dto;

import com.rutina.rutinabackend.domain.dailytarget.entity.DailyTarget;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DailyTargetResponse {

    private Long routineId;
    private LocalDate targetDate;
    private Boolean isCompleted;

    // 엔티티 → 응답 DTO 변환 정적 팩토리
    public static DailyTargetResponse from(DailyTarget dailyTarget) {
        return DailyTargetResponse.builder()
                .routineId(dailyTarget.getRoutine().getId())
                .targetDate(dailyTarget.getTargetDate())
                .isCompleted(dailyTarget.getIsCompleted())
                .build();
    }
}
