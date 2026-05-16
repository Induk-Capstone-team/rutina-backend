package com.rutina.rutinabackend.global.scheduler;

import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;

    // 매일 자정(00:00) 실행
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteWithdrawnUsers() {
        // cutoff(현재 시각 - 7일): deleted_at이 이보다 이른 탈퇴 유저를 영구 삭제
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(7);
        int deleted = userRepository.deleteWithdrawnBefore(cutoff);
        log.info("[UserCleanupScheduler] 탈퇴 유저 완전 삭제 완료: {}명", deleted);
    }
}
