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

    @Column(name = "job")
    private String job;

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

    public void updateNickname(String nickname) {
        // 엔티티 내부에서 닉네임을 변경하도록 메서드로 분리했습니다.
        // 이렇게 하면 서비스에서 필드를 직접 수정하지 않아도 됩니다.
        this.nickname = nickname;
        this.updatedAt = OffsetDateTime.now();
    }

    public void changePassword(String encodedNewPassword) {
        this.password = encodedNewPassword;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateProfile(String job, String age, Integer gender) {
        if (job != null)    this.job = job;
        if (age != null)    this.age = age;
        if (gender != null) this.gender = gender;
        this.updatedAt = OffsetDateTime.now();
    }

    // 실제 레코드 삭제가 아닌 deleted_at 시각 기록
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

}
