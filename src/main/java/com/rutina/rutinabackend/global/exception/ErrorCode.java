package com.rutina.rutinabackend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_001", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_003", "존재하지 않는 사용자입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "유효하지 않거나 만료된 리프레시 토큰입니다."),

    // 루틴
    ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTINE_001", "루틴을 찾을 수 없습니다."),

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    // 사용 예: throw ErrorCode.USER_NOT_FOUND.toException();
    public BusinessException toException() {
        return new BusinessException(status, code, message);
    }
}