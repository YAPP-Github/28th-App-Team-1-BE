package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSessionRepository {

    InterviewSession save(InterviewSession interviewSession);

    Optional<InterviewSession> findById(Long id);

    Optional<InterviewSession> findByIdForUpdate(Long id);

    List<InterviewSession> findAllByUserId(UUID userId);

    boolean existsByPortfolioIdAndStatus(UUID portfolioId, InterviewSessionStatus status);

    long countByUserIdAndAbandonCause(UUID userId, AbandonCause abandonCause);

    // 리포트 없이 끝나 interview_video row가 없는 세션 — S3 잔여물을 추적할 주체가 없어 정리 배치가 직접 찾는다.
    List<InterviewSession> findFileCleanupTargets(LocalDateTime endedBefore, int limit);
}
