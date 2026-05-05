package com.rutina.rutinabackend.domain.routine.dto;

public enum RepeatType {
    NONE,       // 반복 없음 - start_at 날짜 하루만 표시
    DAILY,      // 매일 반복
    WEEKLY,     // 매주 반복 - repeat_days 필수, 1개만 허용 (빠른 선택용)
                // 여러 요일 선택은 CUSTOM + WEEK 사용
    MONTHLY,    // 매월 start_at 기준 같은 날
    YEARLY,     // 매년 start_at 기준 같은 월·일
    WEEKDAYS,   // 매주 평일(월~금) - repeat_days 저장 불필요
    CUSTOM      // 사용자 정의 간격 - repeat_interval + repeat_unit 필수
                // WEEK 단위면 repeat_days도 필수
}
