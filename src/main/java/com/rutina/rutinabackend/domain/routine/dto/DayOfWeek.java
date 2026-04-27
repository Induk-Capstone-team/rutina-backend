package com.rutina.rutinabackend.domain.routine.dto;

// java.time.DayOfWeek와 이름 충돌 방지를 위해 직접 정의
// DB에는 "MON,WED,FRI" 형태 문자열로 저장
public enum DayOfWeek {
    SUN, MON, TUE, WED, THU, FRI, SAT
}
