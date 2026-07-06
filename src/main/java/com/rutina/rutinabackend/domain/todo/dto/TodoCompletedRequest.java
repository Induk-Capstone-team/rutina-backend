package com.rutina.rutinabackend.domain.todo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TodoCompletedRequest {

    @NotNull(message = "완료 여부는 필수입니다.")
    private Boolean completed;
}