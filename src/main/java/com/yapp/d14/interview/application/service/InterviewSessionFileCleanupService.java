package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionFileCleanupUseCase;
import com.yapp.d14.interview.application.port.out.FileCleanupTarget;
import com.yapp.d14.interview.application.port.out.InterviewSessionFileCleaner;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewSessionFileCleanupService implements InterviewSessionFileCleanupUseCase {

    private static final List<InterviewSessionStatus> REPORTLESS_END_STATUSES =
            List.of(InterviewSessionStatus.INVALID, InterviewSessionStatus.PRELOAD_FAILED);
    private static final AbandonCause REPORT_TRIGGERING_CAUSE = AbandonCause.USER_EXIT;

    private static final Duration CLEANUP_GRACE = Duration.ofHours(1);
    private static final int BATCH_SIZE = 500;

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionFileCleaner interviewSessionFileCleaner;

    @Override
    public int cleanupOrphanFiles() {
        List<FileCleanupTarget> targets = interviewSessionRepository.findFileCleanupTargets(
                REPORTLESS_END_STATUSES,
                REPORT_TRIGGERING_CAUSE,
                LocalDateTime.now().minus(CLEANUP_GRACE),
                BATCH_SIZE
        );
        if (targets.isEmpty()) {
            return 0;
        }

        List<Long> cleanedSessionIds = new ArrayList<>();
        for (FileCleanupTarget target : targets) {
            if (deleteFiles(target)) {
                cleanedSessionIds.add(target.sessionId());
            }
        }
        if (!cleanedSessionIds.isEmpty()) {
            interviewSessionRepository.markFilesCleaned(cleanedSessionIds, LocalDateTime.now());
        }

        log.info("[SESSION CLEANUP] 고아 파일 정리 완료: 대상={}, 정리={}", targets.size(), cleanedSessionIds.size());
        return cleanedSessionIds.size();
    }

    private boolean deleteFiles(FileCleanupTarget target) {
        try {
            int deleted = interviewSessionFileCleaner.deleteSessionFiles(target.userId(), target.sessionId());
            log.info("[SESSION CLEANUP] sessionId={}, status={}, 삭제={}건",
                    target.sessionId(), target.status(), deleted);
            return true;
        } catch (Exception e) {
            log.error("[SESSION CLEANUP] 정리 실패, 다음 실행에서 재시도함: sessionId={}", target.sessionId(), e);
            return false;
        }
    }
}
