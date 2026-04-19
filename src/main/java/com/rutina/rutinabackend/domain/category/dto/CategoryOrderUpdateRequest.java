package com.rutina.rutinabackend.domain.category.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CategoryOrderUpdateRequest {

    // 드래그 후 최종 순서대로 category id 목록을 전달
    // 예: [3, 1, 2]
    // - 3번 카테고리 -> sortOrder 0
    // - 1번 카테고리 -> sortOrder 1
    // - 2번 카테고리 -> sortOrder 2
    @NotEmpty(message = "카테고리 순서 정보는 비어 있을 수 없습니다.")
    private List<Long> categoryIds;
}