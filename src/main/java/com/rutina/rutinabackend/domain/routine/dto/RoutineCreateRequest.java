package com.rutina.rutinabackend.domain.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class RoutineCreateRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private Long categoryId;           // 카테고리 ID - 필수값, 미분류 없음

    @NotBlank(message = "루틴 제목은 필수입니다.")
    @Size(max = 30, message = "루틴 제목은 30자 이하로 입력해주세요.")
    private String title;              // 루틴 제목 - DB varchar(30) 제한에 맞춤

    @NotNull(message = "알람 여부는 필수입니다.")
    private Boolean alarm;

    @NotNull(message = "반복 유형은 필수입니다.")
    private RepeatType repeatType;     // NONE/DAILY/WEEKLY/MONTHLY/YEARLY/WEEKDAYS/CUSTOM

    private Integer repeatInterval;    // 반복 간격 숫자 - CUSTOM일 때만 사용 ex) 2 → "매 2주마다"

    private RepeatUnit repeatUnit;     // 반복 단위 - CUSTOM일 때만 사용 (DAY/WEEK/MONTH/YEAR)

    private List<DayOfWeek> repeatDays;// 반복 요일 배열 - WEEKLY 또는 CUSTOM+WEEK일 때 필수. 나머지는 무시

    private LocalTime startTime;

    private LocalTime endTime;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startAt;         // 루틴 시작일 - 필수값. 프론트에서 반드시 전송.

    private LocalDate endAt;           // 루틴 종료일 - null이면 종료일 없음
}
