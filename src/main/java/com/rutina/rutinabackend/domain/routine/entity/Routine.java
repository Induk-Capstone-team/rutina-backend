package com.rutina.rutinabackend.domain.routine.entity;

import com.rutina.rutinabackend.domain.category.entity.Category;
import com.rutina.rutinabackend.domain.routine.dto.RepeatType;
import com.rutina.rutinabackend.domain.routine.dto.RepeatUnit;
import com.rutina.rutinabackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "routines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false)
    private Boolean alarm;          // 알람 사용 여부 (true = 알람 활성화)

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false, length = 10)
    private RepeatType repeatType;  // 반복 유형. UI 화면 구분용 (NONE/DAILY/WEEKLY/MONTHLY/YEARLY/WEEKDAYS/CUSTOM)

    @Column(name = "repeat_interval")
    private Integer repeatInterval; // 반복 간격. CUSTOM일 때만 사용 ex) 2 → 매 2주마다

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_unit", length = 10)
    private RepeatUnit repeatUnit;  // 반복 단위. CUSTOM일 때만 사용 (DAY/WEEK/MONTH/YEAR)

    @Column(name = "repeat_days", length = 50)
    private String repeatDays;      // 반복 요일. WEEKLY 또는 CUSTOM+WEEK일 때만 사용 ex) "MON,WED,FRI"

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;      // 루틴 시작일. NOT NULL, 미입력 시 오늘 날짜 자동 설정

    @Column(name = "end_at")
    private LocalDate endAt;        // 루틴 종료일. null이면 종료일 없음 (무기한 반복)

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    // ── 루틴 생성용 정적 팩토리 ──────────────────────────────────────
    // startAt은 서비스에서 null 처리 후 전달되므로 항상 non-null
    public static Routine create(User user, Category category, String title,
                                 Boolean alarm, RepeatType repeatType,
                                 Integer repeatInterval, RepeatUnit repeatUnit, String repeatDays,
                                 LocalTime startTime, LocalTime endTime,
                                 LocalDate startAt, LocalDate endAt) {
        Routine routine = new Routine();
        routine.user = user;
        routine.category = category;
        routine.title = title;
        routine.alarm = alarm;
        routine.repeatType = repeatType;
        routine.repeatInterval = repeatInterval;
        routine.repeatUnit = repeatUnit;
        routine.repeatDays = repeatDays;
        routine.startTime = startTime;
        routine.endTime = endTime;
        routine.startAt = startAt;
        routine.endAt = endAt;
        routine.createdAt = OffsetDateTime.now();
        routine.updatedAt = OffsetDateTime.now();
        return routine;
    }

    // ── 루틴 전체 수정 ─────────────────────────────────────────────
    // 반복 관련 4개 필드(type/interval/unit/days)를 포함한 전체 필드 갱신
    // updatedAt 자동 갱신
    public void update(Category category, String title, Boolean alarm,
                       RepeatType repeatType, Integer repeatInterval,
                       RepeatUnit repeatUnit, String repeatDays,
                       LocalTime startTime, LocalTime endTime,
                       LocalDate startAt, LocalDate endAt) {
        this.category = category;
        this.title = title;
        this.alarm = alarm;
        this.repeatType = repeatType;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.repeatDays = repeatDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = OffsetDateTime.now();
    }
}
