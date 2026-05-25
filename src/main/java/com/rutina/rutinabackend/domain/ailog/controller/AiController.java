package com.rutina.rutinabackend.domain.ailog.controller;

import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineAddRequest;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineAddResponse;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineOptionValuesResponse;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineRecommendRequest;
import com.rutina.rutinabackend.domain.ailog.dto.AiRoutineRecommendResponse;
import com.rutina.rutinabackend.domain.ailog.service.AiService;
import com.rutina.rutinabackend.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-routines")
@RequiredArgsConstructor
@Tag(name = "AI Routine", description = "AI 루틴 추천 API")
public class AiController {

    private final AiService aiService;

    @Operation(
            summary = "AI 루틴 추천 조건 선택지 조회",
            description = """
                    AI 루틴 추천 요청에 사용할 선택지 목록을 조회합니다.
                    
                    프론트에서는 이 API로 목적, 주요 활동 시간, 활동 타입, 취미 선택지를 받아
                    사용자가 직접 입력하지 않고 정해진 선택지 중에서 고를 수 있도록 처리합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 루틴 추천 조건 선택지 조회 성공"
            )
    })
    @GetMapping("/options")
    public ApiResponse<AiRoutineOptionValuesResponse> getOptions() {
        return ApiResponse.ok(
                "AI 루틴 선택지 조회에 성공했습니다.",
                aiService.getOptions()
        );
    }

    @Operation(
            summary = "AI 루틴 추천 생성",
            description = """
                    로그인한 사용자의 직업, 성별, 나이대와 사용자가 선택한 조건을 기반으로
                    AI가 루틴 후보 5개를 추천합니다.
                    
                    일반 사용자는 계정당 하루 3회까지만 AI 추천을 요청할 수 있습니다.
                    기준 시간은 한국 시간 기준 매일 00:00에 초기화됩니다.
                    
                    ADMIN 계정은 하루 요청 횟수 제한에서 제외됩니다.
                    
                    추천 결과는 바로 루틴으로 저장되지 않고, recommendationId와 함께 후보 목록으로 반환됩니다.
                    이후 사용자가 원하는 후보를 선택하여 추가 API를 호출해야 실제 루틴으로 저장됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 루틴 추천 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청값 오류 또는 사용자 프로필 정보 부족"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "AI 일일 요청 횟수 초과",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "AI_DAILY_LIMIT_EXCEEDED",
                                    value = """
                                            {
                                              "success": false,
                                              "code": "AI_DAILY_LIMIT_EXCEEDED",
                                              "message": "AI 추천은 하루에 3번까지만 요청할 수 있습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "AI 응답 변환 실패 또는 추천 결과 오류"
            )
    })
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

    @Operation(
            summary = "오늘 추천받은 AI 루틴 기록 조회",
            description = """
                로그인한 사용자가 오늘 AI에게 추천받은 루틴 기록을 조회합니다.

                이 API는 과거 추천 기록 조회용이므로 AI를 새로 호출하지 않고,
                하루 3회 추천 횟수도 차감하지 않습니다.

                응답의 각 항목은 AI 루틴 추천 생성 API와 같은
                recommendationId, categoryId, routines 구조로 반환됩니다.
                따라서 프론트에서는 추천 생성 결과 화면과 같은 컴포넌트를 재사용할 수 있습니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "오늘 추천받은 AI 루틴 기록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @GetMapping("/recommendations/today")
    public ApiResponse<List<AiRoutineRecommendResponse>> getTodayRecommendations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.ok(
                "오늘 추천받은 루틴 조회에 성공했습니다.",
                aiService.getTodayRecommendations(userDetails)
        );
    }

    @Operation(
            summary = "선택한 AI 추천 루틴 추가",
            description = """
                    AI 추천 결과 중 사용자가 선택한 루틴만 실제 routines 테이블에 추가합니다.
                    
                    recommendationId는 AI 루틴 추천 생성 API에서 반환된 값입니다.
                    selectedOptionIds에는 사용자가 체크한 optionId 목록을 전달합니다.
                    
                    categoryId는 요청 body에서 전달한 값을 우선 사용합니다.
                    요청 body에 categoryId가 없으면 추천 요청 당시 저장된 categoryId를 사용합니다.
                    둘 다 없으면 루틴을 추가할 수 없습니다.
                    
                    이미 같은 제목의 루틴이 존재하는 경우 중복 추가하지 않습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "선택한 AI 추천 루틴 추가 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 선택값, 카테고리 오류 또는 추가할 루틴 미선택"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "AI 추천 기록을 찾을 수 없음"
            )
    })


    @PostMapping("/{recommendationId}/add")
    public ApiResponse<AiRoutineAddResponse> addRecommendedRoutines(
            @AuthenticationPrincipal UserDetails userDetails,

            @Parameter(
                    description = "AI 루틴 추천 생성 API에서 반환된 추천 기록 ID",
                    example = "1"
            )
            @PathVariable Long recommendationId,

            @Valid @RequestBody AiRoutineAddRequest request
    ) {
        return ApiResponse.ok(
                "선택한 루틴 추가에 성공했습니다.",
                aiService.addRecommendedRoutines(userDetails, recommendationId, request)
        );
    }
}