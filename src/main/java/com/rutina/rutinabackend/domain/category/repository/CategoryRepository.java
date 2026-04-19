package com.rutina.rutinabackend.domain.category.repository;

import com.rutina.rutinabackend.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 내 카테고리 목록 조회
    List<Category> findAllByUser_IdOrderBySortOrderAscIdAsc(Long userId);

    // 내 카테고리 단건 조회
    Optional<Category> findByIdAndUser_Id(Long categoryId, Long userId);

    // 생성 시 이름 중복 체크
    boolean existsByUser_IdAndName(Long userId, String name);

    // 수정 시 자기 자신 제외 이름 중복 체크
    boolean existsByUser_IdAndNameAndIdNot(Long userId, String name, Long categoryId);
}