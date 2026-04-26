package com.rutina.rutinabackend.domain.ailog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rutina.rutinabackend.domain.ailog.dto.*;
import com.rutina.rutinabackend.domain.ailog.repository.AiLogRepository;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

@Service
public class AiService {
    // 나중에 AI 기능이 여러 개로 늘어났을 때, 루틴 추천 로그만 구분하기 위해 사용한다.
    private static final String REQUEST_TYPE_ROUTINE_RECOMMEND = "ROUTINE_RECOMMEND";

    // 프론트에서 자유 입력 거부하고, 하단 선택지에서만 고르게 하기 위한 목록(임시)
    private static final List<String> PURPOSES = List.of(
            "건강 관리",
            "자기계발",
            "공부/집중",
            "생활 습관 개선",
            "취미 관리"
    );

    // 목적
    private static final List<String> MAIN_ACTIVITY_TIMES = List.of(
            "아침",
            "오전",
            "오후",
            "저녁",
            "밤"
    );

    // 루틴 활동 타입
    private static final List<String> ACTIVITY_TYPES = List.of(
            "실내 활동",
            "야외 활동",
            "정적인 활동",
            "동적인 활동",
            "혼자 하는 활동",
            "함께 하는 활동"
    );

    // 취미
    private static final List<String> HOBBIES = List.of(
            "독서",
            "운동",
            "음악",
            "영화/드라마",
            "게임",
            "요리",
            "산책",
            "일기",
            "공부",
            "청소/정리",
            "없음"
    );

    // AI가 추천할 수 있는 루틴 소요 시간 목록
    private static final List<Integer> ALLOWED_DURATIONS = List.of(5, 10, 15, 20, 30);

    // 의존성 주입 필드

    private final ChatClient chatClient; //OpenAI 호출용
    private final UserRepository userRepository; //로그인 유저 조회용
    private final AiLogRepository aiLogRepository; //AI 추천 결과 저장/조회용
    private final JdbcTemplate jdbcTemplate; //카테고리 검증, 루틴 insert 등 직접 SQL 처리용
    private final ObjectMapper objectMapper; //prompt/response JSON 변환용

    public AiService(
            ChatClient.Builder chatClientBuilder,
            UserRepository userRepository,
            AiLogRepository aiLogRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.aiLogRepository = aiLogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // AI 추천 조건 선택지 조회
    public AiRoutineOptionValuesResponse getOptions() {
        return new AiRoutineOptionValuesResponse(
                PURPOSES,
                MAIN_ACTIVITY_TIMES,
                ACTIVITY_TYPES,
                HOBBIES
        );
    }

    // 현재 로그인한 사용자 조회
    private User getLoginUser(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "로그인이 필요합니다."
            );
        }

        String username = userDetails.getUsername();

        try {
            Long userId = Long.valueOf(username);

            return userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "사용자를 찾을 수 없습니다."
                    ));
        } catch (NumberFormatException ignored) {
            Long userId = findUserIdByEmail(username);

            return userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "사용자를 찾을 수 없습니다."
                    ));
        }
    }

    // UserDetails.username이 email인 경우 users 테이블에서 id 조회
    private Long findUserIdByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(
                    "select id from users where email = ?",
                    Long.class,
                    email
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "USER_NOT_FOUND",
                    "사용자를 찾을 수 없습니다."
            );
        }
    }

    // AI 추천에 필요한 사용자 정보 검증
    private void validateUserInfo(User user) {
        boolean missingJob = user.getJob() == null || user.getJob().isBlank();
        boolean missingAge = user.getAge() == null;
        boolean missingGender = user.getGender() == null;

        if (missingJob || missingAge || missingGender) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "AI_PROFILE_INFO_REQUIRED",
                    "직업, 연령, 성별 정보를 입력해야 이용 가능한 서비스입니다."
            );
        }
    }

    // 프론트 선택값 검증
    private void validateRequestOptions(AiRoutineRecommendRequest request) {
        if (!PURPOSES.contains(request.purpose())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PURPOSE",
                    "허용되지 않은 목적입니다."
            );
        }

        if (!MAIN_ACTIVITY_TIMES.contains(request.mainActivityTime())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MAIN_ACTIVITY_TIME",
                    "허용되지 않은 주요 활동 시간입니다."
            );
        }

        if (request.activityType() != null
                && !request.activityType().isBlank()
                && !ACTIVITY_TYPES.contains(request.activityType())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ACTIVITY_TYPE",
                    "허용되지 않은 활동 타입입니다."
            );
        }

        if (request.hobbies() == null || request.hobbies().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_HOBBY",
                    "취미는 최소 1개 이상 선택해야 합니다."
            );
        }

        for (String hobby : request.hobbies()) {
            if (!HOBBIES.contains(hobby)) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_HOBBY",
                        "허용되지 않은 취미입니다."
                );
            }
        }
    }

    public AiRoutineRecommendResponse recommend(UserDetails userDetails, AiRoutineRecommendRequest request) {
        throw new UnsupportedOperationException("AI 루틴 추천 기능 구현 전");
    }

    public AiRoutineAddResponse addRecommendedRoutines(
            UserDetails userDetails,
            Long recommendationId,
            AiRoutineAddRequest request
    ) {
        throw new UnsupportedOperationException("AI 추천 루틴 추가 기능 구현 전");
    }
}