package com.rutina.rutinabackend.domain.category.dto;

import com.rutina.rutinabackend.domain.category.entity.Category;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    // 카테고리 식별자
    private Long id;

    // 카테고리 이름
    private String name;

    // 색상 코드
    private String colorCode;

    // 카테고리에 속한 루틴 집계값
    private String rtSum;

    // 실제 표시 순서
    // 목록 조회 시 프론트가 현재 순서를 알 수 있도록 응답에 포함
    private Integer sortOrder;

    // Entity -> Response DTO 변환
    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .colorCode(category.getColorCode())
                .rtSum(category.getRtSum())
                .sortOrder(category.getSortOrder())
                .hidden(category.getHidden())
                .build();
    }
}