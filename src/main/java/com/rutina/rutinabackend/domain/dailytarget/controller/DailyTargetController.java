package com.rutina.rutinabackend.domain.dailytarget.controller;

import com.rutina.rutinabackend.domain.dailytarget.dto.DailyTargetResponse;
import com.rutina.rutinabackend.domain.dailytarget.service.DailyTargetService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routines")
@Tag(name = "DailyTarget", description = "루틴 완료 API")
public class DailyTargetController {

    private final DailyTargetService dailyTargetService;

    @Operation(summary = "루틴 완료/비완료 토글")
    @PostMapping("/{routineId}/daily-targets/toggle")
    public ApiResponse<DailyTargetResponse> toggle(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long routineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        DailyTargetResponse response = dailyTargetService.toggle(userId, routineId, date);
        return ApiResponse.ok("루틴 완료 상태가 변경되었습니다.", response);
    }
}
