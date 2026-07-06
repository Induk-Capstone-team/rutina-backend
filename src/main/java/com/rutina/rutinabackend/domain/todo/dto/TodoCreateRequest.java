package com.rutina.rutinabackend.domain.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class TodoCreateRequest {

    @NotNull(message = "날짜는 필수입니다.")
    private LocalDate todoDate;

    private LocalTime todoTime;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 30, message = "내용은 최대 30자까지 입력할 수 있습니다.")
    private String content;
}