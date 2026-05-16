package com.rutina.rutinabackend.domain.routine.repository;

import com.rutina.rutinabackend.domain.routine.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {

    // 전체 목록 조회
    List<Routine> findByUserId(Long userId);

    // 히트맵 응답에 카테고리 색상 정보가 필요하므로 category를 함께 조회
    @Query("SELECT r FROM Routine r LEFT JOIN FETCH r.category WHERE r.user.id = :userId")
    List<Routine> findByUserIdWithCategory(@Param("userId") Long userId);

    // 카테고리별 조회
    List<Routine> findByUserIdAndCategoryId(Long userId, Long categoryId);

    // 단건 조회 - findById 대신 userId까지 검증해서 타인 루틴 접근 차단
    Optional<Routine> findByIdAndUserId(Long id, Long userId);

    // 유효 기간 1차 필터 - start_at <= date <= end_at 범위의 루틴만 반환
    // repeat_type 기반 2단계 필터링은 서비스 레이어에서 isActiveOnDate()로 처리
    @Query("SELECT r FROM Routine r WHERE r.user.id = :userId " +
           "AND r.startAt <= :date " +
           "AND (r.endAt IS NULL OR r.endAt >= :date)")
    List<Routine> findActiveByUserIdAndDate(@Param("userId") Long userId,
                                            @Param("date") LocalDate date);

    // 카테고리 + 유효 기간 1차 필터
    // repeat_type 기반 2단계 필터링은 서비스 레이어에서 isActiveOnDate()로 처리
    @Query("SELECT r FROM Routine r WHERE r.user.id = :userId AND r.category.id = :categoryId " +
           "AND r.startAt <= :date " +
           "AND (r.endAt IS NULL OR r.endAt >= :date)")
    List<Routine> findActiveByUserIdAndCategoryIdAndDate(@Param("userId") Long userId,
                                                         @Param("categoryId") Long categoryId,
                                                         @Param("date") LocalDate date);

    long countByUserIdAndCategoryId(Long userId, Long categoryId);

    void deleteByUserIdAndCategoryId(Long userId, Long categoryId);
}
