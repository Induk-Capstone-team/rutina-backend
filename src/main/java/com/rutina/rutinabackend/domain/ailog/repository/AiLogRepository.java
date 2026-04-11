package com.rutina.rutinabackend.domain.ailog.repository;

import com.rutina.rutinabackend.domain.ailog.entity.AiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiLogRepository extends JpaRepository<AiLog, Long> {
    List<AiLog> findByUserId(Long userId);
    List<AiLog> findByRoutineId(Long routineId);
}