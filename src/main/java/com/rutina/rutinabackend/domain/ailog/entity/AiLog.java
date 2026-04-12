package com.rutina.rutinabackend.domain.ailog.entity;

import com.rutina.rutinabackend.domain.routine.entity.Routine;
import com.rutina.rutinabackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @Column(name = "request_type", nullable = false)
    private String requestType;     // 요청 유형

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;      // AI에 전달된 프롬프트

    @Column(nullable = false, columnDefinition = "text")
    private String response;        // AI 응답 내용

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    // ── AI 로그 생성용 정적 팩토리 ──────────────────────────
    public static AiLog create(User loginUser, Routine routine, String requestType, String prompt, String response) {
        AiLog aiLog = new AiLog();
        aiLog.user = loginUser;
        aiLog.routine = routine;
        aiLog.requestType = requestType;
        aiLog.prompt = prompt;
        aiLog.response = response;
        aiLog.createdAt = OffsetDateTime.now();
        aiLog.updatedAt = OffsetDateTime.now();
        return aiLog;
    }
}