package com.rutina.rutinabackend.domain.ailog.controller;

import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineAddRequest;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineAddResponse;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineOptionValuesResponse;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineRecommendRequest;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineRecommendResponse;
//import com.rutina.rutinabackend.domain.ailog.dto.AiRequest;
//import com.rutina.rutinabackend.domain.ailog.dto.AiResponse;
import com.rutina.rutinabackend.domain.ailog.service.AiService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-routines")
@RequiredArgsConstructor
@Tag(name = "AI Routine", description = "AI 루틴 추천 API")
public class AiController {

    // 루틴 추천 조건 선택지 조회 API
    private final AiService aiService;

    @Operation(summary = "AI 루틴 추천 조건 선택지 조회")
    @GetMapping("/options")
    public ApiResponse<AiRoutineOptionValuesResponse> getOptions() {
        return ApiResponse.ok(
                "AI 루틴 선택지 조회에 성공했습니다.",
                aiService.getOptions()
        );
    }

    // AI 루틴 추천 생성 API
    @Operation(summary = "AI 루틴 추천 생성")
    @PostMapping("/recommend")
    public ApiResponse<AiRoutineRecommendResponse> recommend(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AiRoutineRecommendRequest request
    ) {
        return ApiResponse.ok(
                "AI 루틴 추천에 성공했습니다.",
                aiService.recommend(userDetails, request)
        );
    }

    // AI 추천 루틴 중 사용자가 선택한 루틴만 추가하는 API
    @Operation(summary = "선택한 AI 추천 루틴 추가")
    @PostMapping("/{recommendationId}/add")
    public ApiResponse<AiRoutineAddResponse> addRecommendedRoutines(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long recommendationId,
            @Valid @RequestBody AiRoutineAddRequest request
    ) {
        return ApiResponse.ok(
                "선택한 루틴 추가에 성공했습니다.",
                aiService.addRecommendedRoutines(userDetails, recommendationId, request)
        );
    }

// 초기 코드 주석 처리
//    @PostMapping("/generate")
//    public ApiResponse<AiResponse> generate(@RequestBody AiRequest request) {
//        AiResponse response = aiService.generate(request);
//        return ApiResponse.ok("AI 응답 생성에 성공했습니다.", response);
//    }
}