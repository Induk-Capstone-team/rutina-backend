package com.rutina.rutinabackend.domain.category.entity;

import com.rutina.rutinabackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "color_code", nullable = false, length = 7)
    private String colorCode;

    @Column(name = "rt_sum", nullable = false, length = 10)
    private String rtSum;   // 카테고리 루틴 집계값

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;  // 정렬 순서

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    // ── 카테고리 생성용 정적 팩토리 ──────────────────────────
    public static Category create(User user, String name, String colorCode, String rtSum, Integer sortOrder) {
        Category category = new Category();
        category.user = user;
        category.name = name;
        category.colorCode = colorCode;
        category.rtSum = rtSum;
        category.sortOrder = sortOrder;
        category.createdAt = OffsetDateTime.now();
        category.updatedAt = OffsetDateTime.now();
        return category;
    }
    // ── 카테고리 수정 시 사용 ──────────────────────────
    public void update(String name, String colorCode, Integer sortOrder) {
        this.name = name;
        this.colorCode = colorCode;
        this.sortOrder = sortOrder;
        this.updatedAt = OffsetDateTime.now();
    }
}
