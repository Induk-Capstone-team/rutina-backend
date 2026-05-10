package com.rutina.rutinabackend.domain.ailog.repository;

import com.rutina.rutinabackend.domain.ailog.entity.AiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiLogRepository extends JpaRepository<AiLog, Long> {
    //기본적인 유저 조회
    List<AiLog> findByUser_Id(Long userId);
    //추가하기 버튼 눌렀을 때, 사용자가 선택한 optionId가 어떤 루틴 제목인지 알기 위함
    Optional<AiLog> findByIdAndUser_Id(Long id, Long userId);

    long countByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            String requestType,
            OffsetDateTime start,
            OffsetDateTime end
    );
}