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

    // 종료 후에도 답변 음성 비동기 저장·지연된 preload TTS·이미 발급된 presigned PUT URL(10분)로
    // 파일이 뒤늦게 도착할 수 있어, 그 창구가 모두 닫힌 뒤를 대상으로 삼는다.
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

    // 한 세션이 실패해도 나머지는 계속 처리하고, 실패한 세션은 마킹하지 않아 다음 실행에서 다시 시도한다.
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
