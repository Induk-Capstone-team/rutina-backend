package com.rutina.rutinabackend.domain.dailytarget.repository;

import com.rutina.rutinabackend.domain.dailytarget.entity.DailyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyTargetRepository extends JpaRepository<DailyTarget, Long> {
    List<DailyTarget> findByRoutineId(Long routineId);
    Optional<DailyTarget> findByRoutineIdAndTargetDate(Long routineId, LocalDate targetDate);
    List<DailyTarget> findByRoutineIdInAndTargetDate(List<Long> routineIds, LocalDate targetDate);

    // 여러 루틴의 기간 내 완료된 기록만 조회
    List<DailyTarget> findByRoutineIdInAndTargetDateBetweenAndIsCompletedTrue(
            List<Long> routineIds,
            LocalDate startDate,
            LocalDate endDate
    );
}
