package com.rutina.rutinabackend.domain.routine.dto;

import com.rutina.rutinabackend.domain.routine.entity.Routine;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoutineConflictResponse {

    private final Long id;
    private final String title;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public static RoutineConflictResponse from(Routine routine) {
        return new RoutineConflictResponse(
                routine.getId(),
                routine.getTitle(),
                routine.getStartTime(),
                routine.getEndTime()
        );
    }
}
