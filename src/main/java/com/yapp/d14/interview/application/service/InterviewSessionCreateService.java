package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionPreloadUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.domain.AxisWeightCalculator;
import com.yapp.d14.interview.domain.AxisWeightCalculator.AxisAssignment;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.ticket.application.port.in.TicketAvailabilityCheckUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewSessionCreateService implements InterviewSessionCreateUseCase {

    private final TicketAvailabilityCheckUseCase ticketAvailabilityCheckUseCase;
    private final InterviewSessionCreateValidator interviewSessionCreateValidator;
    private final InterviewSessionPersister interviewSessionPersister;
    private final InterviewSessionPreloadUseCase interviewSessionPreloadUseCase;
    private final InterviewPreloadFailureHandler interviewPreloadFailureHandler;

    @Override
    public InterviewSessionCreateResult create(InterviewSessionCreateCommand command) {
        ticketAvailabilityCheckUseCase.checkAvailable(command.userId());

        interviewSessionCreateValidator.validate(command);

        Map<TestType, Integer> weights = AxisWeightCalculator.compute(command.jobRole(), command.careerYears());
        Map<TestType, AxisAssignment> assignments = AxisWeightCalculator.assignTierAndBudget(weights);

        InterviewSession session = interviewSessionPersister.persist(command, weights, assignments);

        triggerPreload(session.getId());

        return new InterviewSessionCreateResult(session.getId(), session.getStatus());
    }

    private void triggerPreload(Long sessionId) {
        try {
            interviewSessionPreloadUseCase.preload(sessionId);
        } catch (RejectedExecutionException e) {
            log.error("[INTERVIEW PRELOAD] 비동기 처리 큐가 가득 찼어요: sessionId={}", sessionId, e);
            interviewPreloadFailureHandler.markFailed(sessionId);
        }
    }
}
