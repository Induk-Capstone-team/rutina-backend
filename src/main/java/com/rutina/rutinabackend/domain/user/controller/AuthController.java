package com.rutina.rutinabackend.domain.user.controller;

import com.rutina.rutinabackend.domain.user.dto.*;
import com.rutina.rutinabackend.domain.user.service.AuthService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로컬 회원가입
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ApiResponse.created("회원가입이 완료되었습니다.", response);
    }

    // 로컬 로그인
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.ok("로그인에 성공했습니다.", response);
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ApiResponse<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean isDuplicate = authService.checkEmailDuplicate(email);
        return ApiResponse.ok(
                isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.",
                Map.of("isDuplicate", isDuplicate)
        );
    }
}
