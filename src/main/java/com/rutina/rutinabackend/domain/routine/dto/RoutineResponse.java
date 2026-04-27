package com.rutina.rutinabackend.domain.routine.dto;

import com.rutina.rutinabackend.domain.routine.entity.Routine;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class RoutineResponse {

    private Long id;
    private Long categoryId;
    private String categoryColorCode;
    private String title;
    private Boolean alarm;
    private RepeatType repeatType;
    private Integer repeatInterval;     // CUSTOM일 때만 값 있음, 나머지는 null
    private RepeatUnit repeatUnit;      // CUSTOM일 때만 값 있음, 나머지는 null
    private List<DayOfWeek> repeatDays; // DB 문자열 "MON,WED" → 배열로 역변환. 없으면 빈 리스트
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startAt;
    private LocalDate endAt;

    // 엔티티 → 응답 DTO 변환 정적 팩토리
    // repeatDays: routine.getRepeatDays()가 null이면 빈 리스트,
    //             아니면 쉼표로 split 후 DayOfWeek::valueOf로 매핑
    public static RoutineResponse from(Routine routine) {
        List<DayOfWeek> repeatDays = routine.getRepeatDays() == null
                ? Collections.emptyList()
                : Arrays.stream(routine.getRepeatDays().split(","))
                        .map(DayOfWeek::valueOf)
                        .collect(Collectors.toList());

        return RoutineResponse.builder()
                .id(routine.getId())
                .categoryId(routine.getCategory().getId())
                .categoryColorCode(routine.getCategory() != null ? routine.getCategory().getColorCode() : null)
                .title(routine.getTitle())
                .alarm(routine.getAlarm())
                .repeatType(routine.getRepeatType())
                .repeatInterval(routine.getRepeatInterval())
                .repeatUnit(routine.getRepeatUnit())
                .repeatDays(repeatDays)
                .startTime(routine.getStartTime())
                .endTime(routine.getEndTime())
                .startAt(routine.getStartAt())
                .endAt(routine.getEndAt())
                .build();
    }
}
