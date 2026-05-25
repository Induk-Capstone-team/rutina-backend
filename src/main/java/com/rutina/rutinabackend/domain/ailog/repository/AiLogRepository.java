package com.rutina.rutinabackend.domain.ailog.repository;

import com.rutina.rutinabackend.domain.ailog.entity.AiLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiLogRepository extends JpaRepository<AiLog, Long> {
    //기본적인 유저 조회
    List<AiLog> findByUser_Id(Long userId);
    //추가하기 버튼 눌렀을 때, 사용자가 선택한 optionId가 어떤 루틴 제목인지 알기 위함
    Optional<AiLog> findByIdAndUser_Id(Long id, Long userId);

    long countByUser_IdAndRequestTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            String requestType,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Query("""
        SELECT a
        FROM AiLog a
        WHERE a.user.id = :userId
          AND a.requestType = :requestType
          AND a.createdAt >= :start
          AND a.createdAt < :end
        ORDER BY a.createdAt DESC
    """)
        List<AiLog> findTodayRecommendations(
                @Param("userId") Long userId,
                @Param("requestType") String requestType,
                @Param("start") OffsetDateTime start,
                @Param("end") OffsetDateTime end
        );

    // 루틴 삭제 전 먼저 호출 필수
    @Modifying
    @Query("UPDATE AiLog a SET a.routine = null WHERE a.routine.id IN :routineIds")
    void anonymizeByRoutineIds(@Param("routineIds") List<Long> routineIds);

    // 유저 삭제 전 먼저 호출 필수
    @Modifying
    @Query("UPDATE AiLog a SET a.user = null WHERE a.user.id = :userId")
    void anonymizeByUserId(@Param("userId") Long userId);

}