package com.rutina.rutinabackend.domain.user.service;

import com.rutina.rutinabackend.domain.ailog.repository.AiLogRepository;
import com.rutina.rutinabackend.domain.category.repository.CategoryRepository;
import com.rutina.rutinabackend.domain.email.service.EmailVerificationService;
import com.rutina.rutinabackend.domain.routine.repository.RoutineRepository;
import com.rutina.rutinabackend.domain.user.dto.NewUserStatusResponse;
import com.rutina.rutinabackend.domain.user.dto.NicknameUpdateRequest;
import com.rutina.rutinabackend.domain.user.dto.NicknameUpdateResponse;
import com.rutina.rutinabackend.domain.user.dto.PasswordChangeRequest;
import com.rutina.rutinabackend.domain.user.dto.UserProfileUpdateRequest;
import com.rutina.rutinabackend.domain.user.dto.UserResponse;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.auth.token.RefreshTokenStore;
import com.rutina.rutinabackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final RoutineRepository routineRepository;
    private final CategoryRepository categoryRepository;
    private final AiLogRepository aiLogRepository;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public NewUserStatusResponse getNewUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);
        return NewUserStatusResponse.from(user);
    }

    @Transactional
    public NicknameUpdateResponse updateNickname(Long userId, NicknameUpdateRequest request) {
        // 로그인한 사용자의 id로 User를 조회합니다.
        // 존재하지 않는 사용자라면 공통 에러 코드로 예외를 발생시킵니다.
        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);

        // 닉네임 중복은 허용하기 때문에 별도의 중복 검사는 하지 않습니다.
        // JPA 변경 감지로 트랜잭션이 끝날 때 update 쿼리가 실행됩니다.
        user.updateNickname(request.nickname());

        // 변경된 닉네임을 바로 응답으로 내려주기 위해 응답 DTO로 변환합니다.
        return NicknameUpdateResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);

        // 로컬 로그인 유저인지 확인
        if (!"LOCAL".equals(user.getProvider())) {
            throw ErrorCode.NOT_LOCAL_USER.toException();
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ErrorCode.WRONG_PASSWORD.toException();
        }

        // 새로운 비밀번호 검증
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw ErrorCode.SAME_AS_CURRENT_PASSWORD.toException();
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);
        user.updateProfile(request.job(), request.age(), request.gender());
        return UserResponse.from(user);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        emailVerificationService.checkPasswordResetVerified(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);

        user.changePassword(passwordEncoder.encode(newPassword));

        emailVerificationService.clearPasswordResetVerified(email);
    }

    @Transactional
    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(ErrorCode.USER_NOT_FOUND::toException);

        // ai_logs.routine_id → null (루틴 삭제 전)
        List<Long> routineIds = routineRepository.findIdsByUserId(userId);
        if (!routineIds.isEmpty()) {
            aiLogRepository.anonymizeByRoutineIds(routineIds);
        }

        // ai_logs.user_id → null (유저 삭제 전)
        aiLogRepository.anonymizeByUserId(userId);

        // routines 삭제 (daily_targets는 CASCADE로 자동 삭제)
        routineRepository.deleteAllByUserId(userId);

        // categories 삭제
        categoryRepository.deleteAllByUser_Id(userId);

        // refresh token 삭제
        refreshTokenStore.deleteAllByUserId(userId);

        // user soft delete (users 레코드는 7일 보관 후 스케줄러가 완전 삭제)
        user.softDelete();
    }
}
