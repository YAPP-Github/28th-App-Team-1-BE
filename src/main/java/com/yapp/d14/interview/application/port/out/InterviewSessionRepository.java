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

    List<FileCleanupTarget> findFileCleanupTargets(
            List<InterviewSessionStatus> reportlessStatuses,
            AbandonCause reportTriggeringCause,
            LocalDateTime endedBefore,
            int limit
    );

    void markFilesCleaned(List<Long> sessionIds, LocalDateTime cleanedAt);
}
