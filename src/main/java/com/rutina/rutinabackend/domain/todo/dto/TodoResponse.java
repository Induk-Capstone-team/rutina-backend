package com.rutina.rutinabackend.domain.todo.dto;

import com.rutina.rutinabackend.domain.todo.entity.Todo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class TodoResponse {

    private Long id;
    private LocalDate todoDate;
    private LocalTime todoTime;
    private String content;
    private Boolean completed;

    public static TodoResponse from(Todo todo) {
        return TodoResponse.builder()
                .id(todo.getId())
                .todoDate(todo.getTodoDate())
                .todoTime(todo.getTodoTime())
                .content(todo.getContent())
                .completed(todo.getCompleted())
                .build();
    }
}