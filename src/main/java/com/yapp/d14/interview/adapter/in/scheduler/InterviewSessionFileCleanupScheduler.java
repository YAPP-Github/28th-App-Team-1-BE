package com.yapp.d14.interview.adapter.in.scheduler;

import com.yapp.d14.interview.application.port.in.InterviewSessionFileCleanupUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class InterviewSessionFileCleanupScheduler {

    private final InterviewSessionFileCleanupUseCase interviewSessionFileCleanupUseCase;

    // 정리가 하루 늦어져도 사용자 영향이 없으므로 트래픽이 가장 적은 새벽에 하루 한 번만 돈다.
    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    void cleanupOrphanFiles() {
        try {
            interviewSessionFileCleanupUseCase.cleanupOrphanFiles();
        } catch (Exception e) {
            // 스케줄러 스레드로 예외가 올라가면 다음 실행까지 조용히 죽으므로 여기서 끊는다.
            log.error("[SESSION CLEANUP] 배치 실행 실패", e);
        }
    }
}
