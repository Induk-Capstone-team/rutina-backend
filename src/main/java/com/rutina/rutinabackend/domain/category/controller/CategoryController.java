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

    @Operation(
            summary = "카테고리 생성",
            description = """
                로그인한 사용자의 새 카테고리를 생성합니다.

                요청 시 카테고리 이름(name)과 색상 코드(colorCode)를 전달합니다.
                새로 생성된 카테고리는 기본적으로 숨김 상태가 아니며(hidden=false),
                sortOrder는 0으로 저장되어 목록의 가장 위에 표시됩니다.

                기존 카테고리들은 sortOrder가 1씩 증가하여 뒤로 밀립니다.
                """
    )
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

    @Operation(
            summary = "카테고리 단건 조회",
            description = """
                특정 카테고리 1개의 상세 정보를 조회합니다.

                path variable로 전달한 categoryId가 로그인한 사용자의 카테고리인 경우에만 조회됩니다.
                다른 사용자의 카테고리이거나 존재하지 않는 카테고리인 경우 조회되지 않습니다.

                """
    )
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

    @Operation(
            summary = "카테고리 수정",
            description = """
                카테고리 이름, 색상 코드, 숨김 여부를 수정합니다.

                hidden=true로 수정하면 기본 카테고리 목록 조회(GET /api/v1/categories)에서 제외됩니다.
                hidden=false로 수정하면 다시 기본 카테고리 목록에 표시됩니다.

                카테고리 순서는 이 API에서 수정하지 않습니다.
                순서 변경은 별도의 카테고리 순서 변경 API를 사용해야 합니다.
                
                """
    )
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

    @Operation(
            summary = "카테고리 순서 변경",
            description = """
                카테고리 목록의 표시 순서를 변경합니다.

                프론트에서 드래그 앤 드롭으로 카테고리 순서를 변경한 뒤,
                최종 순서대로 categoryIds 배열을 전달합니다.

                배열의 첫 번째 id는 sortOrder=0,
                두 번째 id는 sortOrder=1,
                세 번째 id는 sortOrder=2로 저장됩니다.

                주의:
                - categoryIds에는 로그인한 사용자의 카테고리 id만 포함되어야 합니다.
                - 현재 화면에 표시 중인 카테고리 순서 그대로 전달해야 합니다.
                - 숨김 카테고리를 기본 목록에서 제외하는 정책이라면, 프론트는 hidden=false 카테고리 id만 전달해야 합니다.

                요청 예시:
                {
                  "categoryIds": [3, 1, 2]
                }
                """
    )
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

    @Operation(
            summary = "카테고리 삭제",
            description = """
                특정 카테고리를 삭제합니다.

                삭제 시 해당 카테고리에 속한 루틴도 함께 삭제됩니다.
                단순히 화면에서 보이지 않게 하려는 목적이라면 삭제 API가 아니라
                카테고리 수정 API에서 hidden=true로 변경해야 합니다.

                주의:
                - 삭제는 복구가 어렵습니다.
                - 프론트에서는 삭제 전 확인 모달을 표시하는 것을 권장합니다.
                - 카테고리만 숨기고 싶다면 PUT /api/v1/categories/{categoryId} 요청에서 hidden=true를 사용합니다.
                
                """
    )
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