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

    @Operation(
            summary = "내 카테고리 목록 조회",
            description = """
                로그인한 사용자의 카테고리 목록을 조회합니다.
                숨김 처리되지 않은 카테고리(hidden=false)만 반환합니다.
                앱의 기본 카테고리 목록 화면에서 사용합니다.
                """
    )
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        List<CategoryResponse> response = categoryService.getCategories(userId);
        return ApiResponse.ok("카테고리 목록 조회에 성공했습니다.", response);
    }

    @Operation(
            summary = "숨김 카테고리 목록 조회",
            description = """
                로그인한 사용자의 숨김 카테고리 목록을 조회합니다.
                hidden=true 상태인 카테고리만 반환합니다.
                숨김 카테고리를 다시 표시 상태로 변경할 때 사용할 수 있습니다.
                """
    )
    @GetMapping("/hidden")
    public ApiResponse<List<CategoryResponse>> getHiddenCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        List<CategoryResponse> response = categoryService.getHiddenCategories(userId);
        return ApiResponse.ok("숨김 카테고리 목록 조회에 성공했습니다.", response);
    }

    @Operation(
            summary = "내 카테고리 전체 조회",
            description = """
                로그인한 사용자의 모든 카테고리를 조회합니다.
                숨김 여부와 관계없이 hidden=true, hidden=false 카테고리를 모두 반환합니다.
                관리자성 확인 또는 카테고리 관리 화면에서 사용할 수 있습니다.
                """
    )
    @GetMapping("/all")
    public ApiResponse<List<CategoryResponse>> getAllCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        List<CategoryResponse> response = categoryService.getAllCategories(userId);
        return ApiResponse.ok("전체 카테고리 목록 조회에 성공했습니다.", response);
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
    @PutMapping("/{categoryId}")
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
    @PutMapping("/reorder")
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