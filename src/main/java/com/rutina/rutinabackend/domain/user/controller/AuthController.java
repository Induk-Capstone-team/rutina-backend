package com.rutina.rutinabackend.domain.user.controller;

import com.rutina.rutinabackend.domain.refreshToken.dto.TokenRefreshRequest;
import com.rutina.rutinabackend.domain.user.dto.*;
import com.rutina.rutinabackend.domain.user.service.AuthService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로컬 회원가입")
    @SecurityRequirements({})
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponse> signUp(@RequestBody @Valid SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ApiResponse.created("회원가입이 완료되었습니다.", response);
    }

    @Operation(summary = "로컬 로그인")
    @SecurityRequirements({})
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                            HttpServletRequest httpRequest) {
        // User-Agent 헤더에서 기기 정보를 추출 후 refreshToken과 함께 저장
        String device = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, device);
        return ApiResponse.ok("로그인에 성공했습니다.", response);
    }

    @Operation(summary = "토큰 재발급")
    @SecurityRequirements({})
    @PostMapping("/reissue")
    public ApiResponse<LoginResponse> reissue(@RequestBody @Valid TokenRefreshRequest request,
                                              HttpServletRequest httpRequest) {
        String device = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.reissue(request, device);
        return ApiResponse.ok("토큰이 재발급되었습니다.", response);
    }

    @Operation(summary = "로그아웃")
    @SecurityRequirements({})
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid TokenRefreshRequest request) {
        authService.logout(request);
        return ApiResponse.ok("로그아웃되었습니다.", null);
    }

    @Operation(summary = "이메일 중복 확인")
    @SecurityRequirements({})
    @GetMapping("/check-email")
    public ApiResponse<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean isDuplicate = authService.checkEmailDuplicate(email);
        return ApiResponse.ok(
                isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.",
                Map.of("isDuplicate", isDuplicate)
        );
    }
}
