package com.rutina.rutinabackend.domain.user.service;

import com.rutina.rutinabackend.domain.user.dto.*;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── 회원가입 ───────────────────────────────────────────
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw ErrorCode.DUPLICATE_EMAIL.toException();
        }

        // 비밀번호 bcrypt 해시
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocal(request.email(), encodedPassword, request.nickname());
        userRepository.save(user);

        return SignUpResponse.from(user);
    }

    // ── 이메일 중복 확인 ─────────────────────────
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}
