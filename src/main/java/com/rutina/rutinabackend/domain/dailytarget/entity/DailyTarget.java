package com.rutina.rutinabackend.domain.dailytarget.entity;

import com.rutina.rutinabackend.domain.routine.entity.Routine;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_targets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;       //목표 날짜

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;        //완료 여부

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    // ── 일일 목표 생성용 정적 팩토리 ──────────────────────────
    public static DailyTarget create(Routine routine, LocalDate targetDate) {
        DailyTarget dailyTarget = new DailyTarget();
        dailyTarget.routine = routine;
        dailyTarget.targetDate = targetDate;
        dailyTarget.isCompleted = false;
        dailyTarget.createdAt = OffsetDateTime.now();
        dailyTarget.updatedAt = OffsetDateTime.now();
        return dailyTarget;
    }
}