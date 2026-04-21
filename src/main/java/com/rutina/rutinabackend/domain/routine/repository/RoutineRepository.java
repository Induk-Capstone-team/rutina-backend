package com.rutina.rutinabackend.domain.routine.repository;

import com.rutina.rutinabackend.domain.routine.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {
    List<Routine> findByUserId(Long userId);
    List<Routine> findByUserIdAndCategoryId(Long userId, Long categoryId);

    long countByUserIdAndCategoryId(Long userId, Long categoryId);

    void deleteByUserIdAndCategoryId(Long userId, Long categoryId);
}