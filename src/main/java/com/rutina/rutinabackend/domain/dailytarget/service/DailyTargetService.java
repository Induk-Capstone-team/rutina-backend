package com.rutina.rutinabackend.domain.dailytarget.service;

import com.rutina.rutinabackend.domain.dailytarget.dto.DailyTargetResponse;
import com.rutina.rutinabackend.domain.dailytarget.entity.DailyTarget;
import com.rutina.rutinabackend.domain.dailytarget.repository.DailyTargetRepository;
import com.rutina.rutinabackend.domain.routine.entity.Routine;
import com.rutina.rutinabackend.domain.routine.repository.RoutineRepository;
import com.rutina.rutinabackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyTargetService {

    private final DailyTargetRepository dailyTargetRepository;
    private final RoutineRepository routineRepository;

    // ── 루틴 완료/비완료 토글 ──────────────────────────────────────
    // 해당 날짜의 DailyTarget이 없으면 새로 생성(isCompleted=true),
    // 있으면 현재 상태를 반전
    @Transactional
    public DailyTargetResponse toggle(Long userId, Long routineId, LocalDate targetDate) {
        Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(ErrorCode.ROUTINE_NOT_FOUND::toException);

        DailyTarget dailyTarget = dailyTargetRepository
                .findByRoutineIdAndTargetDate(routineId, targetDate)
                .map(dt -> {
                    dt.toggleCompleted();
                    return dt;
                })
                .orElseGet(() -> {
                    DailyTarget created = DailyTarget.create(routine, targetDate);
                    created.toggleCompleted(); // false → true
                    return dailyTargetRepository.save(created);
                });

        return DailyTargetResponse.from(dailyTarget);
    }
}
