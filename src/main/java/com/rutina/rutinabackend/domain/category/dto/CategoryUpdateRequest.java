package com.rutina.rutinabackend.domain.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CategoryUpdateRequest {

    // 카테고리 수정 시 이름 검증
    @NotBlank(message = "카테고리 이름은 필수입니다.")
    @Size(max = 20, message = "카테고리 이름은 20자 이하로 입력해주세요.")
    private String name;

    // 카테고리 수정 시 색상 코드 검증
    @NotBlank(message = "색상 코드는 필수입니다.")
    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "색상 코드는 #RRGGBB 형식이어야 합니다."
    )
    private String colorCode;
}