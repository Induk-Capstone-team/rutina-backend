package com.rutina.rutinabackend.domain.ailog.constant;

import java.util.List;

public final class AiRoutineOptionConstants {

    private AiRoutineOptionConstants() {
    }

    public static final List<String> PURPOSES = List.of(
            "건강 관리",
            "자기계발",
            "공부/집중",
            "생활 습관 개선",
            "취미 관리"
    );

    public static final List<String> MAIN_ACTIVITY_TIMES = List.of(
            "아침",
            "오전",
            "오후",
            "저녁",
            "밤"
    );

    public static final List<String> ACTIVITY_TYPES = List.of(
            "실내 활동",
            "야외 활동",
            "정적인 활동",
            "동적인 활동",
            "혼자 하는 활동",
            "함께 하는 활동"
    );

    public static final List<String> HOBBIES = List.of(
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

    public static final List<Integer> ALLOWED_DURATIONS = List.of(
            10, 20, 30, 40, 50, 60
    );
}