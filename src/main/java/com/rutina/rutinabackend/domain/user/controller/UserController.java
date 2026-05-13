package com.rutina.rutinabackend.domain.user.controller;

import com.rutina.rutinabackend.domain.user.dto.NicknameUpdateRequest;
import com.rutina.rutinabackend.domain.user.dto.NicknameUpdateResponse;
import com.rutina.rutinabackend.domain.user.dto.PasswordChangeRequest;
import com.rutina.rutinabackend.domain.user.dto.PasswordResetRequest;
import com.rutina.rutinabackend.domain.user.dto.UserResponse;
import com.rutina.rutinabackend.domain.user.dto.UserProfileUpdateRequest;
import com.rutina.rutinabackend.domain.user.service.UserService;
import com.rutina.rutinabackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        UserResponse response = userService.getMe(userId);
        return ApiResponse.ok("내 정보를 불러왔습니다.", response);
    }

    @Operation(summary = "닉네임 변경")
    @PatchMapping("/me/nickname")
    public ApiResponse<NicknameUpdateResponse> updateNickname(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        // JWT 필터에서 인증이 끝나면 UserDetails.username에 userId가 문자열로 들어가 있습니다.
        // 그래서 서비스로 넘기기 전에 Long 타입으로 변환해서 사용합니다.
        Long userId = Long.parseLong(userDetails.getUsername());

        // 실제 닉네임 변경 로직은 서비스에서 처리하고, 컨트롤러는 요청/응답만 담당합니다.
        NicknameUpdateResponse response = userService.updateNickname(userId, request);
        return ApiResponse.ok("닉네임이 변경되었습니다.", response);
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        userService.changePassword(userId, request);
        return ApiResponse.ok("비밀번호가 변경되었습니다.", null);
    }

    @Operation(
            summary = "프로필 정보 수정(닉네임 변경X)",
            description = "null로 전달된 필드는 기존 값을 그대로 유지합니다.\n\n" +
                          "**gender**: 0 = 남성, 1 = 여성"
    )
    @PatchMapping("/me/profile")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        UserResponse response = userService.updateProfile(userId, request);
        return ApiResponse.ok("프로필 정보가 업데이트되었습니다.", response);
    }

    @Operation(summary = "비밀번호 재설정", description = "이메일 인증 완료 후 새 비밀번호로 재설정합니다. 반드시 /api/v1/email/password-reset/confirm 인증을 먼저 완료해야 합니다.")
    @SecurityRequirements({})
    @PostMapping("/password-reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request.email(), request.newPassword());
        return ApiResponse.ok("비밀번호가 재설정되었습니다.", null);
    }
}
