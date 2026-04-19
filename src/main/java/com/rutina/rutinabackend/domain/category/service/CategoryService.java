package com.rutina.rutinabackend.domain.category.service;

import com.rutina.rutinabackend.domain.category.dto.CategoryCreateRequest;
import com.rutina.rutinabackend.domain.category.dto.CategoryOrderUpdateRequest;
import com.rutina.rutinabackend.domain.category.dto.CategoryResponse;
import com.rutina.rutinabackend.domain.category.dto.CategoryUpdateRequest;
import com.rutina.rutinabackend.domain.category.entity.Category;
import com.rutina.rutinabackend.domain.category.repository.CategoryRepository;
import com.rutina.rutinabackend.domain.routine.repository.RoutineRepository;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RoutineRepository routineRepository;

    // 카테고리 생성
    // 정책:
    // - 새 카테고리는 항상 맨 위에 오도록 sortOrder = 0
    // - 기존 카테고리들은 sortOrder를 1씩 뒤로 밀어냄
    @Transactional
    public CategoryResponse createCategory(Long userId, CategoryCreateRequest request) {
        String categoryName = request.getName().trim();

        // 같은 사용자 기준 이름 중복 방지
        if (categoryRepository.existsByUser_IdAndName(userId, categoryName)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CATEGORY_409", "이미 같은 이름의 카테고리가 존재합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."));

        // 기존 카테고리를 전부 뒤로 한 칸씩 밀어서
        // 새 카테고리가 0번 순서로 들어갈 수 있게 함
        List<Category> categories = categoryRepository.findAllByUser_IdOrderBySortOrderAscIdAsc(userId);
        for (Category category : categories) {
            category.updateSortOrder(category.getSortOrder() + 1);
        }

        Category newCategory = Category.create(
                user,
                categoryName,
                request.getColorCode(),
                "0",
                0
        );

        Category savedCategory = categoryRepository.save(newCategory);
        return CategoryResponse.from(savedCategory);
    }

    // 내 카테고리 전체 조회
    public List<CategoryResponse> getCategories(Long userId) {
        return categoryRepository.findAllByUser_IdOrderBySortOrderAscIdAsc(userId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    // 내 카테고리 단건 조회
    public CategoryResponse getCategory(Long userId, Long categoryId) {
        Category category = findCategory(userId, categoryId);
        return CategoryResponse.from(category);
    }

    // 카테고리 수정
    // 이름, 색상만 수정하고 순서는 건드리지 않음
    @Transactional
    public CategoryResponse updateCategory(Long userId, Long categoryId, CategoryUpdateRequest request) {
        Category category = findCategory(userId, categoryId);
        String categoryName = request.getName().trim();

        if (categoryRepository.existsByUser_IdAndNameAndIdNot(userId, categoryName, categoryId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CATEGORY_409", "이미 같은 이름의 카테고리가 존재합니다.");
        }

        category.update(
                categoryName,
                request.getColorCode()
        );

        return CategoryResponse.from(category);
    }

    // 카테고리 순서 변경
    // 프론트가 드래그 후 최종 순서대로 id 목록 전체를 보내면
    // 그 순서대로 sortOrder를 다시 저장
    @Transactional
    public void updateCategoryOrder(Long userId, CategoryOrderUpdateRequest request) {
        List<Category> categories = categoryRepository.findAllByUser_IdOrderBySortOrderAscIdAsc(userId);
        List<Long> requestIds = request.getCategoryIds();

        // 사용자의 전체 카테고리 개수와 요청 개수가 다르면 잘못된 요청
        if (categories.size() != requestIds.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CATEGORY_400", "잘못된 카테고리 순서 요청입니다.");
        }

        // 요청으로 들어온 id들이 정말 이 사용자의 카테고리 id 목록과 일치하는지 검증
        List<Long> ownedIds = categories.stream()
                .map(Category::getId)
                .sorted()
                .toList();

        List<Long> sortedRequestIds = requestIds.stream()
                .sorted()
                .toList();

        if (!ownedIds.equals(sortedRequestIds)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CATEGORY_400", "잘못된 카테고리 순서 요청입니다.");
        }

        // id -> Category 매핑
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        // 프론트가 보낸 순서대로 sortOrder 재설정
        for (int i = 0; i < requestIds.size(); i++) {
            Category category = categoryMap.get(requestIds.get(i));
            category.updateSortOrder(i);
        }
    }

    // 카테고리 삭제
    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = findCategory(userId, categoryId);

        // 해당 카테고리에 속한 루틴도 함께 삭제
        routineRepository.deleteByUserIdAndCategoryId(userId, categoryId);

        // 그 다음 카테고리 삭제
        categoryRepository.delete(category);
    }

    // 공통 조회 메서드
    // 항상 userId까지 같이 걸어서 본인 카테고리만 접근 가능하게 함
    private Category findCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CATEGORY_404", "카테고리를 찾을 수 없습니다."));
    }
}