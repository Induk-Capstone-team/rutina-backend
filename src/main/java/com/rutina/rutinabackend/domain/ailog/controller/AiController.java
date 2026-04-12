package com.rutina.rutinabackend.domain.ailog.controller;

import com.rutina.rutinabackend.domain.ailog.dto.AiRequest;
import com.rutina.rutinabackend.domain.ailog.dto.AiResponse;
import com.rutina.rutinabackend.domain.ailog.service.AiService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    public ApiResponse<AiResponse> generate(@RequestBody AiRequest request) {
        AiResponse response = aiService.generate(request);
        return ApiResponse.ok("AI 응답 생성에 성공했습니다.", response);
    }
}