package com.rutina.rutinabackend.domain.routine.controller;

import com.rutina.rutinabackend.domain.routine.dto.RoutineCreateRequest;
import com.rutina.rutinabackend.domain.routine.dto.RoutineResponse;
import com.rutina.rutinabackend.domain.routine.dto.RoutineTimetableResponse;
import com.rutina.rutinabackend.domain.routine.dto.RoutineUpdateRequest;
import com.rutina.rutinabackend.domain.routine.service.RoutineService;
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
@RequestMapping("/api/v1/routines")
@Tag(name = "Routine", description = "루틴 API")
public class RoutineController {

    private final RoutineService routineService;

    @Operation(summary = "루틴 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoutineResponse> createRoutine(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RoutineCreateRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        RoutineResponse response = routineService.createRoutine(userId, request);
        return ApiResponse.created("루틴이 생성되었습니다.", response);
    }

    @Operation(summary = "루틴 목록 조회")
    @GetMapping
    public ApiResponse<List<RoutineResponse>> getRoutines(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // categoryId, date 모두 optional이며 4가지 조합(null/null, null/값, 값/null, 값/값) 모두 동작
        Long userId = Long.parseLong(userDetails.getUsername());

        List<RoutineResponse> response = routineService.getRoutines(userId, categoryId, date);
        return ApiResponse.ok("루틴 목록 조회에 성공했습니다.", response);
    }

    @Operation(summary = "타임테이블 조회")
    @GetMapping("/timetable")
    public ApiResponse<List<RoutineTimetableResponse>> getTimetable(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // date는 required=true(기본값)이므로 미전송 시 Spring이 자동으로 400 반환
        Long userId = Long.parseLong(userDetails.getUsername());

        List<RoutineTimetableResponse> response = routineService.getTimetable(userId, date);
        return ApiResponse.ok("타임테이블 조회에 성공했습니다.", response);
    }

    @Operation(summary = "루틴 단건 조회")
    @GetMapping("/{routineId}")
    public ApiResponse<RoutineResponse> getRoutine(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long routineId
    ) {
        // path variable로 받은 routineId와 로그인 userId를 함께 사용해서 본인 루틴만 조회 가능하도록 처리
        Long userId = Long.parseLong(userDetails.getUsername());

        RoutineResponse response = routineService.getRoutine(userId, routineId);
        return ApiResponse.ok("루틴 조회에 성공했습니다.", response);
    }

    @Operation(summary = "루틴 수정")
    @PutMapping("/{routineId}")
    public ApiResponse<RoutineResponse> updateRoutine(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineUpdateRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        RoutineResponse response = routineService.updateRoutine(userId, routineId, request);
        return ApiResponse.ok("루틴이 수정되었습니다.", response);
    }

    @Operation(summary = "루틴 삭제")
    @DeleteMapping("/{routineId}")
    public ApiResponse<Void> deleteRoutine(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long routineId
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        routineService.deleteRoutine(userId, routineId);
        return ApiResponse.ok("루틴이 삭제되었습니다.", null);
    }
}
