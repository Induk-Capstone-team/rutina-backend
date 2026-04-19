package com.rutina.rutinabackend.domain.category.controller;

import com.rutina.rutinabackend.domain.category.dto.CategoryCreateRequest;
import com.rutina.rutinabackend.domain.category.dto.CategoryOrderUpdateRequest;
import com.rutina.rutinabackend.domain.category.dto.CategoryResponse;
import com.rutina.rutinabackend.domain.category.dto.CategoryUpdateRequest;
import com.rutina.rutinabackend.domain.category.service.CategoryService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@Tag(name = "Category", description = "카테고리 API")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        // 현재 프로젝트의 인증 구조에서는
        // UserDetails.username 자리에 userId가 문자열로 들어가 있으므로 Long으로 변환해서 사용
        Long userId = Long.parseLong(userDetails.getUsername());

        // 카테고리 생성 서비스 호출
        CategoryResponse response = categoryService.createCategory(userId, request);

        // 공통 응답 포맷으로 반환
        return ApiResponse.created("카테고리가 생성되었습니다.", response);
    }

    @Operation(summary = "내 카테고리 목록 조회")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // accessToken 인증 사용자 기준으로 본인 카테고리만 조회
        Long userId = Long.parseLong(userDetails.getUsername());

        List<CategoryResponse> response = categoryService.getCategories(userId);
        return ApiResponse.ok("카테고리 목록 조회에 성공했습니다.", response);
    }

    @Operation(summary = "카테고리 단건 조회")
    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long categoryId
    ) {
        // path variable로 받은 categoryId와 로그인 userId를 함께 사용해서
        // 본인 카테고리만 조회 가능하도록 처리
        Long userId = Long.parseLong(userDetails.getUsername());

        CategoryResponse response = categoryService.getCategory(userId, categoryId);
        return ApiResponse.ok("카테고리 조회에 성공했습니다.", response);
    }

    @Operation(summary = "카테고리 수정")
    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        // 카테고리 이름, 색상 수정
        // 순서는 여기서 수정하지 않고 reorder API에서만 변경
        Long userId = Long.parseLong(userDetails.getUsername());

        CategoryResponse response = categoryService.updateCategory(userId, categoryId, request);
        return ApiResponse.ok("카테고리가 수정되었습니다.", response);
    }

    @Operation(summary = "카테고리 순서 변경")
    @PatchMapping("/reorder")
    public ApiResponse<Void> reorderCategories(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CategoryOrderUpdateRequest request
    ) {
        // 프론트에서 드래그 후 최종 순서대로 categoryIds 배열을 보내면
        // 서비스에서 해당 순서대로 sortOrder를 다시 저장
        Long userId = Long.parseLong(userDetails.getUsername());

        categoryService.updateCategoryOrder(userId, request);
        return ApiResponse.ok("카테고리 순서가 변경되었습니다.", null);
    }

    @Operation(summary = "카테고리 삭제")
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long categoryId
    ) {
        // 카테고리 삭제 전 프론트에서 한 번 더 확인 모달을 띄우는 것을 전제로 함
        // 실제 삭제 로직은 서비스에서 처리
        Long userId = Long.parseLong(userDetails.getUsername());

        categoryService.deleteCategory(userId, categoryId);
        return ApiResponse.ok("카테고리가 삭제되었습니다.", null);
    }
}