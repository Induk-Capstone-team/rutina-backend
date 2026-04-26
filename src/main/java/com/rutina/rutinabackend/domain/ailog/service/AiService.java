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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rutina.rutinabackend.domain.ailog.entity.AiLog;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // AI 프롬포트 생성
    private String buildRecommendPrompt(
            User user,
            AiRoutineRecommendRequest request,
            String categoryName,
            String format
    ) {
        return """
            아래 정보를 바탕으로 루틴 후보 5개를 추천해라.

            [사용자 정보]
            직업: %s
            성별: %s
            나이대: %s

            [선택한 카테고리]
            카테고리: %s

            [사용자 선택값]
            목적: %s
            주요 활동 시간: %s
            활동 타입: %s
            취미: %s

            [생성 규칙]
            - 루틴은 정확히 5개만 생성한다.
            - optionId는 1, 2, 3, 4, 5로 생성한다.
            - title은 30자 이하로 작성한다.
            - description은 생성하지 않는다.
            - recommendedTime은 사용자의 주요 활동 시간과 어울리게 작성한다.
            - durationMinutes는 5, 10, 15, 20, 30 중 하나만 사용한다.
            - 반복 여부는 만들지 않는다.
            - 사용자가 바로 추가할 수 있는 구체적인 루틴 제목으로 만든다.

            [응답 형식]
            %s
            """.formatted(
                user.getJob(),
                convertGender(user.getGender()),
                String.valueOf(user.getAge()),
                normalizeBlank(categoryName, "선택 안 함"),
                request.purpose(),
                request.mainActivityTime(),
                normalizeBlank(request.activityType(), "선택 안 함"),
                String.join(", ", request.hobbies()),
                format
        );
    }

    /**
     * AI 루틴 추천 생성
     *
     * 처리 흐름:
     * 1. 로그인 유저 조회
     * 2. 유저 테이블의 직업, 성별, 나이대 검증
     * 3. 사용자가 선택한 목적, 주요 활동 시간, 활동 타입, 취미 검증
     * 4. categoryId가 있으면 본인 카테고리인지 확인하고 카테고리명 조회
     * 5. OpenAI에 전달할 프롬프트 생성
     * 6. OpenAI 호출
     * 7. AI 응답을 DTO로 변환
     * 8. 추천 결과를 5개로 정리
     * 9. AiLog에 요청 정보와 추천 결과 저장
     * 10. recommendationId와 루틴 후보 5개 반환
     */

    @Transactional
    public AiRoutineRecommendResponse recommend(
            UserDetails userDetails,
            AiRoutineRecommendRequest request
    ) {
        User user = getLoginUser(userDetails);

        validateUserInfo(user);
        validateRequestOptions(request);

        String categoryName = null; //협의 전까지 선택값으로 둠.

        // 추천 요청 단계에서 categoryId 들어오면 해당 카테고리가 유저의 카테고리인지 확인, 조회
        if (request.categoryId() != null) {
            categoryName = findCategoryName(user.getId(), request.categoryId());
        }

        /*
         * AI 응답을 지정한 DTO 형태로 변환하기 위해 사용한다.
         *
         * 최종적인 AI 응답 구조
         *
         * {
         *   "routines": [
         *     {
         *       "optionId": 1,
         *       "title": "아침 물 한 잔 마시기",
         *       "recommendedTime": "아침",
         *       "durationMinutes": 5
         *     }
         *   ]
         * }
         */

        BeanOutputConverter<AiRoutineRecommendResult> converter =
                new BeanOutputConverter<>(AiRoutineRecommendResult.class);

        String prompt = buildRecommendPrompt(
                user,
                request,
                categoryName,
                converter.getFormat()
        );

        String aiAnswer = chatClient.prompt()
                .system("""
                    너는 루틴 추천 앱 Rutina의 AI 루틴 추천 엔진이다.
                    사용자의 직업, 성별, 나이대와 선택 조건을 바탕으로 루틴 후보를 추천한다.
                    설명(description)은 절대 생성하지 않는다.
                    반복 설정은 생성하지 않는다.
                    사용자가 실제 앱에 추가할 수 있는 루틴 제목만 추천한다.
                    """)
                .user(prompt)
                .call()
                .content();

        AiRoutineRecommendResult parsedResult = convertAiResult(converter, aiAnswer);
        AiRoutineRecommendResult normalizedResult = normalizeResult(parsedResult);

        /*
         * AiLog.prompt에는 추천 요청 당시의 사용자 정보와 선택 조건 저장
         * AiLog.response에는 AI 추천 결과 5개 저장
         */

        AiRoutinePromptLog promptLog = createPromptLog(user, request, categoryName);

        AiLog aiLog = AiLog.create(
                user,
                null,
                REQUEST_TYPE_ROUTINE_RECOMMEND,
                toJson(promptLog),
                toJson(normalizedResult)
        );

        aiLogRepository.save(aiLog);

        return new AiRoutineRecommendResponse(
                aiLog.getId(),
                request.categoryId(),
                normalizedResult.routines()
        );
    }

    /**
     * AI 추천 결과 정리
     *
     * 보정 내용:
     * - 추천 결과가 5개 미만이면 예외
     * - 5개보다 많으면 앞에서 5개만 사용
     * - optionId는 서버에서 1~5로 다시 부여
     * - title은 30자 제한
     * - recommendedTime이 비어 있으면 "미정"
     * - durationMinutes가 허용 목록에 없으면 10분
     */

    private AiRoutineRecommendResult normalizeResult(AiRoutineRecommendResult result) {
        List<AiRoutineOption> routines = result.routines();

        if (routines == null || routines.size() < 5) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI_RESPONSE_COUNT_INVALID",
                    "AI 추천 결과는 5개여야 합니다."
            );
        }

        List<AiRoutineOption> normalized = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            AiRoutineOption option = routines.get(i);
            String title = normalizeTitle(option.title());

            if (title.isBlank()) {
                throw new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "AI_RESPONSE_TITLE_INVALID",
                        "AI 추천 루틴 제목이 올바르지 않습니다."
                );
            }

            normalized.add(new AiRoutineOption(
                    i + 1,
                    title,
                    normalizeBlank(option.recommendedTime(), "미정"),
                    normalizeDuration(option.durationMinutes())
            ));
        }

        return new AiRoutineRecommendResult(normalized);
    }

    // AiLog.prompt에 저장할 추천 요청 정보 생성
    private AiRoutinePromptLog createPromptLog(
            User user,
            AiRoutineRecommendRequest request,
            String categoryName
    ) {
        return new AiRoutinePromptLog(
                user.getId(),
                user.getJob(),
                convertGender(user.getGender()),
                String.valueOf(user.getAge()),
                request.categoryId(),
                categoryName,
                request.purpose(),
                request.mainActivityTime(),
                normalizeBlank(request.activityType(), "선택 안 함"),
                request.hobbies()
        );
    }

    // categoryId로 카테고리명 조회
    private String findCategoryName(Long userId, Long categoryId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select name from categories where id = ? and user_id = ?",
                    String.class,
                    categoryId,
                    userId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY",
                    "본인의 카테고리만 선택할 수 있습니다."
            );
        }
    }

    // 성별 값 문자열 변환
    // gender가 숫자인 경우: 0 → 남성 / 1 → 여성 / 2 → 기타

    private String convertGender(Object gender) {
        if (gender == null) {
            return "미입력";
        }

        if (gender instanceof Number number) {
            return switch (number.intValue()) {
                case 0 -> "남성";
                case 1 -> "여성";
                case 2 -> "기타";
                default -> "미입력";
            };
        }

        String value = gender.toString().trim();

        if (value.isBlank()) {
            return "미입력";
        }

        return value;
    }

    // 루틴 제목 길이 제한 정리
    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }

        String normalized = title.trim();

        if (normalized.length() > 30) {
            return normalized.substring(0, 30);
        }

        return normalized;
    }

    // null 또는 빈 문자열이면 기본값으로 대체
    private String normalizeBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    // 추천 소요 시간 정리: AI가 이상한 값 내려 주면 그 값 보정
    private Integer normalizeDuration(Integer durationMinutes) {
        if (durationMinutes == null) {
            return 10;
        }

        if (!ALLOWED_DURATIONS.contains(durationMinutes)) {
            return 10;
        }

        return durationMinutes;
    }
    // AI 응답 문자열을 AiRoutineRecommendResult DTO로 변환
    private AiRoutineRecommendResult convertAiResult(
            BeanOutputConverter<AiRoutineRecommendResult> converter,
            String aiAnswer
    ) {
        try {
            AiRoutineRecommendResult result = converter.convert(aiAnswer);

            if (result == null || result.routines() == null) {
                throw new IllegalStateException("AI 응답이 비어 있습니다.");
            }

            return result;
        } catch (Exception e) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI_RESPONSE_PARSE_FAILED",
                    "AI 추천 결과 변환에 실패했습니다."
            );
        }
    }

    // 객체를 JSON 문자열로 변환
    // AiLog.prompt, AiLog.response 저장 시 사용
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON_CONVERT_FAILED",
                    "JSON 변환에 실패했습니다."
            );
        }
    }

    /**
     * 사용자가 체크한 AI 추천 루틴을 실제 routines 테이블에 추가
     *
     * 처리 흐름:
     * 1. 로그인 유저 조회
     * 2. recommendationId로 AiLog 조회
     * 3. categoryId 결정
     *    - 추가 요청의 categoryId 우선 사용
     *    - 없으면 추천 요청 당시 AiLog.prompt에 저장된 categoryId 사용
     *    - 둘 다 없으면 예외 발생
     * 4. AiLog.response에서 AI 추천 루틴 5개 복원
     * 5. 사용자가 체크한 optionId만 찾아 routines 테이블에 insert
     */
    @Transactional
    public AiRoutineAddResponse addRecommendedRoutines(
            UserDetails userDetails,
            Long recommendationId,
            AiRoutineAddRequest request
    ) {
        User user = getLoginUser(userDetails);

        AiLog aiLog = aiLogRepository.findByIdAndUser_Id(recommendationId, user.getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "AI_RECOMMENDATION_NOT_FOUND",
                        "AI 추천 기록을 찾을 수 없습니다."
                ));

        Long categoryId = resolveCategoryId(user.getId(), request.categoryId(), aiLog);

        AiRoutineRecommendResult result = fromJson(aiLog.getResponse(), AiRoutineRecommendResult.class);

        Map<Integer, AiRoutineOption> optionMap = result.routines().stream()
                .collect(Collectors.toMap(AiRoutineOption::optionId, option -> option));

        List<Integer> selectedOptionIds = request.selectedOptionIds().stream()
                .distinct()
                .toList();

        if (selectedOptionIds.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "AI_ROUTINE_SELECTION_REQUIRED",
                    "추가할 루틴을 최소 1개 이상 선택해야 합니다."
            );
        }

        int addedCount = 0;

        for (Integer optionId : selectedOptionIds) {
            AiRoutineOption option = optionMap.get(optionId);

            if (option == null) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_AI_ROUTINE_OPTION",
                        "존재하지 않는 추천 루틴입니다."
                );
            }

            insertRoutine(user.getId(), categoryId, option.title());
            addedCount++;
        }

        return new AiRoutineAddResponse(addedCount);
    }

    /**
     * routines insert에 사용할 categoryId 결정
     *
     * 우선순위:
     * 1. 추가 요청 body의 categoryId
     * 2. 추천 요청 당시 AiLog.prompt에 저장된 categoryId
     * 3. 둘 다 없으면 예외
     */
    private Long resolveCategoryId(Long userId, Long requestCategoryId, AiLog aiLog) {
        if (requestCategoryId != null) {
            validateCategoryOwnership(userId, requestCategoryId);
            return requestCategoryId;
        }

        AiRoutinePromptLog promptLog = fromJson(aiLog.getPrompt(), AiRoutinePromptLog.class);

        if (promptLog.categoryId() != null) {
            validateCategoryOwnership(userId, promptLog.categoryId());
            return promptLog.categoryId();
        }

        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "CATEGORY_REQUIRED",
                "루틴을 추가하려면 카테고리를 선택해야 합니다."
        );
    }

    /**
     * 선택한 카테고리가 현재 로그인한 유저의 카테고리인지 검증
     */
    private void validateCategoryOwnership(Long userId, Long categoryId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists(select 1 from categories where id = ? and user_id = ?)",
                Boolean.class,
                categoryId,
                userId
        );

        if (exists == null || !exists) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY",
                    "본인의 카테고리만 선택할 수 있습니다."
            );
        }
    }

    /**
     * 선택된 추천 루틴을 routines 테이블에 추가
     *
     * 반복은 기본적으로 하지 않으므로 cron_expression은 null로 저장한다.
     */
    private void insertRoutine(Long userId, Long categoryId, String title) {
        jdbcTemplate.update("""
            insert into routines
            (user_id, category_id, title, alarm, state, cron_expression, start_time, end_time, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            """,
                userId,
                categoryId,
                title,
                false,
                true,
                null,
                null,
                null
        );
    }

    /**
     * JSON 문자열을 객체로 변환
     *
     * AiLog.prompt, AiLog.response를 다시 DTO로 복원할 때 사용한다.
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON_PARSE_FAILED",
                    "JSON 파싱에 실패했습니다."
            );
        }
    }
}