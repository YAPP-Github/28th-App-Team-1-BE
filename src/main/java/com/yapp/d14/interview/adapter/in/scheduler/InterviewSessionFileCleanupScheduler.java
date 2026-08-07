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

    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    void cleanupOrphanFiles() {
        try {
            interviewSessionFileCleanupUseCase.cleanupOrphanFiles();
        } catch (Exception e) {
            log.error("[SESSION CLEANUP] 배치 실행 실패", e);
        }
    }
}
