package com.rutina.rutinabackend.domain.todo.controller;

import com.rutina.rutinabackend.domain.todo.dto.*;
import com.rutina.rutinabackend.domain.todo.service.TodoService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/todos")
@Tag(name = "Todo", description = "캘린더 Todo API")
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "Todo 생성", description = "선택한 날짜에 Todo를 생성합니다. 시간은 선택 입력이며 내용은 최대 30자까지 입력할 수 있습니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TodoResponse> createTodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TodoCreateRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.created("Todo가 생성되었습니다.", todoService.createTodo(userId, request));
    }

    @Operation(summary = "날짜별 Todo 조회", description = "특정 날짜에 등록된 Todo 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<TodoResponse>> getTodosByDate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.ok("Todo 목록 조회에 성공했습니다.", todoService.getTodosByDate(userId, date));
    }

    @Operation(summary = "오늘 Todo 조회", description = "오늘 날짜에 등록된 Todo 목록을 조회합니다.")
    @GetMapping("/today")
    public ApiResponse<List<TodoResponse>> getTodayTodos(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.ok("오늘 Todo 목록 조회에 성공했습니다.", todoService.getTodayTodos(userId));
    }

    @Operation(summary = "월별 Todo 조회", description = "캘린더 표시를 위해 특정 연월에 등록된 Todo 목록을 조회합니다.")
    @GetMapping("/month")
    public ApiResponse<List<TodoResponse>> getTodosByMonth(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.ok("월별 Todo 목록 조회에 성공했습니다.", todoService.getTodosByMonth(userId, year, month));
    }

    @Operation(summary = "Todo 수정", description = "Todo의 날짜, 시간, 내용을 수정합니다.")
    @PutMapping("/{todoId}")
    public ApiResponse<TodoResponse> updateTodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.ok("Todo가 수정되었습니다.", todoService.updateTodo(userId, todoId, request));
    }

    @Operation(summary = "Todo 완료 상태 변경", description = "Todo의 완료 여부를 체크 또는 해제합니다.")
    @PatchMapping("/{todoId}/completed")
    public ApiResponse<TodoResponse> updateCompleted(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoCompletedRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ApiResponse.ok("Todo 완료 상태가 변경되었습니다.", todoService.updateCompleted(userId, todoId, request));
    }

    @Operation(summary = "Todo 삭제", description = "등록된 Todo를 삭제합니다.")
    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> deleteTodo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long todoId
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        todoService.deleteTodo(userId, todoId);
        return ApiResponse.ok("Todo가 삭제되었습니다.", null);
    }
}