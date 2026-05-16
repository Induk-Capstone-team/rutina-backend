package com.rutina.rutinabackend.domain.user.repository;

import com.rutina.rutinabackend.domain.user.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // soft delete된 유저까지 포함하여 조회
    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    // deleted_at이 cutoff 이전인 탈퇴 유저를 완전 삭제
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM users WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff", nativeQuery = true)
    int deleteWithdrawnBefore(@Param("cutoff") OffsetDateTime cutoff);

}
