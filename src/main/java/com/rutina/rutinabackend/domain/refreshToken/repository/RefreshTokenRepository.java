package com.rutina.rutinabackend.domain.refreshToken.repository;

import com.rutina.rutinabackend.domain.refreshToken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    @Transactional
    void deleteByUserIdAndDevice(Long userId, String device);

    @Transactional
    void deleteAllByUserId(Long userId);
}
