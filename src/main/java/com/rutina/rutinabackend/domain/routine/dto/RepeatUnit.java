package com.rutina.rutinabackend.domain.routine.dto;

public enum RepeatUnit {
    DAY,    // N일마다
    WEEK,   // N주마다 (repeat_days 함께 사용)
    MONTH,  // N개월마다
    YEAR    // N년마다
}
