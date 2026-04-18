package com.rutina.rutinabackend.domain.refreshToken.entity;

import com.rutina.rutinabackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_value", nullable = false, columnDefinition = "text")
    private String tokenValue;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "device")
    private String device;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    public static RefreshToken of(User user, String tokenValue, long expiryMs, String device) {
        RefreshToken token = new RefreshToken();
        token.user = user;
        token.tokenValue = tokenValue;
        token.expiresAt = OffsetDateTime.now().plusNanos(expiryMs * 1_000_000L);
        token.device = device;
        token.createdAt = OffsetDateTime.now();
        token.updatedAt = OffsetDateTime.now();
        return token;
    }
}
