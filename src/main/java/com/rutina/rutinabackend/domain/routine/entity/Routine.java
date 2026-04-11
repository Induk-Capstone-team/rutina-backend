package com.rutina.rutinabackend.domain.routine.entity;


import com.rutina.rutinabackend.domain.category.entity.Category;
import com.rutina.rutinabackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Boolean alarm;      //알람 사용 여부

    @Column(nullable = false)
    private Boolean state;      //루틴 활성 여부

    @Column(name = "cron_expression")
    private String cronExpression;      //반복  스케줄 표현식

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    // ── 루틴 생성용 정적 팩토리 ──────────────────────────
    public static Routine create(User loginUser, Category category, String title, Boolean alarm, Boolean state, String cronExpression, LocalTime startTime, LocalTime endTime) {
        Routine routine = new Routine();
        routine.user = loginUser;
        routine.category = category;
        routine.title = title;
        routine.alarm = alarm;
        routine.state = state;
        routine.cronExpression = cronExpression;
        routine.startTime = startTime;
        routine.endTime = endTime;
        routine.createdAt = OffsetDateTime.now();
        routine.updatedAt = OffsetDateTime.now();
        return routine;
    }
}
