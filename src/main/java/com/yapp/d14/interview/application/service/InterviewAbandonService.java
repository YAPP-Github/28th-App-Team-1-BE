package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewAbandonCommand;
import com.yapp.d14.interview.application.port.in.InterviewAbandonUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAbandonResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewAbandonService implements InterviewAbandonUseCase {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewAbandonPersister interviewAbandonPersister;

    @Override
    public InterviewAbandonResult abandon(UUID userId, InterviewAbandonCommand command) {
        InterviewSession session = InterviewSessionAccessSupport.requireOwned(
                interviewSessionRepository, command.sessionId(), userId
        );
        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new InterviewException(InterviewErrorCode.SESSION_ALREADY_ENDED);
        }

        InterviewAbandonPersister.PersistResult persisted = interviewAbandonPersister.persist(session, command.cause());

        return new InterviewAbandonResult(
                session.getId(),
                InterviewSessionStatus.ABANDONED.name(),
                command.cause(),
                persisted.endedAt(),
                persisted.ticketOutcome(),
                persisted.reportGenerating()
        );
    }
}
