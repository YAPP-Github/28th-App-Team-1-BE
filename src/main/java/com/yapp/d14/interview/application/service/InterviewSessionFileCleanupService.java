package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionFileCleanupUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionFileCleaner;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewSessionFileCleanupService implements InterviewSessionFileCleanupUseCase {

    private static final Duration CLEANUP_GRACE = Duration.ofHours(1);
    private static final int BATCH_SIZE = 500;

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionFileCleaner interviewSessionFileCleaner;

    @Override
    public int cleanupOrphanFiles() {
        List<InterviewSession> targets = interviewSessionRepository.findFileCleanupTargets(
                LocalDateTime.now().minus(CLEANUP_GRACE), BATCH_SIZE
        );
        if (targets.isEmpty()) {
            return 0;
        }

        int cleaned = 0;
        for (InterviewSession session : targets) {
            if (cleanup(session)) {
                cleaned++;
            }
        }
        log.info("[SESSION CLEANUP] 고아 파일 정리 완료: 대상={}, 정리={}", targets.size(), cleaned);
        return cleaned;
    }

    private boolean cleanup(InterviewSession session) {
        try {
            int deleted = interviewSessionFileCleaner.deleteSessionFiles(session.getUserId(), session.getId());
            session.markFilesCleaned();
            interviewSessionRepository.save(session);
            log.info("[SESSION CLEANUP] sessionId={}, status={}, 삭제={}건",
                    session.getId(), session.getStatus(), deleted);
            return true;
        } catch (Exception e) {
            log.error("[SESSION CLEANUP] 정리 실패, 다음 실행에서 재시도함: sessionId={}", session.getId(), e);
            return false;
        }
    }
}
