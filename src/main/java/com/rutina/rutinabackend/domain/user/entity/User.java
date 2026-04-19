package com.rutina.rutinabackend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")   // soft delete - 조회 시 자동 필터만
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String nickname;

    private String age;

    @Column(nullable = false)
    private String role;

    private Integer gender;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 일반 회원가입용 정적 팩토리 메서드
    public static User createLocal(String email, String encodedPassword, String nickname) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.nickname = nickname;
        user.role = "USER";
        user.provider = "LOCAL";
        user.createdAt = OffsetDateTime.now();
        user.updatedAt = OffsetDateTime.now();
        return user;
    }

    // 소셜 로그인 정적 팩토리 메서드
    public static User createOAuth(String email, String nickname, String provider, String providerId) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.role = "USER";
        user.provider = provider;
        user.providerId = providerId;
        user.createdAt = OffsetDateTime.now();
        user.updatedAt = OffsetDateTime.now();
        return user;
    }

}
