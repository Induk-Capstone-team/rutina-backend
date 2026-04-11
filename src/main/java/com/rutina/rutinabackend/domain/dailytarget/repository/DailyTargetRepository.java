package com.rutina.rutinabackend.domain.dailytarget.repository;

import com.rutina.rutinabackend.domain.dailytarget.entity.DailyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyTargetRepository extends JpaRepository<DailyTarget, Long> {
    List<DailyTarget> findByRoutineId(Long routineId);
    List<DailyTarget> findByRoutineIdAndTargetDate(Long routineId, LocalDate targetDate);
}