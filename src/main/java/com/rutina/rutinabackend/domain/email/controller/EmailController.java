package com.rutina.rutinabackend.domain.email.controller;

import com.rutina.rutinabackend.domain.email.dto.EmailCodeVerifyRequest;
import com.rutina.rutinabackend.domain.email.dto.EmailVerificationRequest;
import com.rutina.rutinabackend.domain.email.service.EmailVerificationService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email", description = "이메일 인증 API")
@RestController
@RequestMapping("/api/v1/auth/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "회원가입 이메일 인증번호 발송", description = "입력한 이메일로 6자리 인증번호를 발송합니다. 인증번호 유효시간은 5분입니다.")
    @SecurityRequirements({})
    @PostMapping("/verification-code")
    public ApiResponse<Void> sendVerificationCode(@RequestBody @Valid EmailVerificationRequest request) {
        emailVerificationService.sendVerificationCode(request.email());
        return ApiResponse.ok("인증번호가 발송되었습니다.", null);
    }

    @Operation(summary = "회원가입 이메일 인증번호 확인", description = "발송된 인증번호를 검증합니다. 인증 성공 시 10분간 인증 완료 상태가 유지되며, 이 시간 안에 회원가입을 완료해야 합니다.")
    @SecurityRequirements({})
    @PostMapping("/verification-code/verify")
    public ApiResponse<Void> verifyCode(@RequestBody @Valid EmailCodeVerifyRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.ok("이메일 인증이 완료되었습니다.", null);
    }

    @Operation(summary = "비밀번호 재설정 인증번호 발송", description = "가입된 이메일로 비밀번호 재설정용 인증번호를 발송합니다. 가입되지 않은 이메일은 오류를 반환합니다. 인증번호 유효시간은 5분입니다.")
    @SecurityRequirements({})
    @PostMapping("/password-reset-code")
    public ApiResponse<Void> sendPasswordResetCode(@RequestBody @Valid EmailVerificationRequest request) {
        emailVerificationService.sendPasswordResetCode(request.email());
        return ApiResponse.ok("인증번호가 발송되었습니다.", null);
    }

    @Operation(summary = "비밀번호 재설정 인증번호 확인", description = "발송된 인증번호를 검증합니다. 인증 성공 시 10분간 인증 완료 상태가 유지되며, 이 시간 안에 비밀번호 재설정을 완료해야 합니다.")
    @SecurityRequirements({})
    @PostMapping("/password-reset-code/verify")
    public ApiResponse<Void> verifyPasswordResetCode(@RequestBody @Valid EmailCodeVerifyRequest request) {
        emailVerificationService.verifyPasswordResetCode(request.email(), request.code());
        return ApiResponse.ok("인증번호 검증이 완료되었습니다.", null);
    }
}
