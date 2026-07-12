package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionPreloadUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AxisWeightCalculator;
import com.yapp.d14.interview.domain.AxisWeightCalculator.AxisAssignment;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.ticket.application.port.in.TicketAvailabilityCheckUseCase;
import com.yapp.d14.ticket.application.port.in.TicketHoldUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewSessionCreateService implements InterviewSessionCreateUseCase {

    private final TicketAvailabilityCheckUseCase ticketAvailabilityCheckUseCase;
    private final TicketHoldUseCase ticketHoldUseCase;
    private final InterviewSessionCreateValidator interviewSessionCreateValidator;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;
    private final InterviewSessionPreloadUseCase interviewSessionPreloadUseCase;
    private final InterviewPreloadFailureHandler interviewPreloadFailureHandler;

    @Override
    @Transactional
    public InterviewSessionCreateResult create(InterviewSessionCreateCommand command) {
        ticketAvailabilityCheckUseCase.checkAvailable(command.userId());

        interviewSessionCreateValidator.validate(command);

        Map<TestType, Integer> weights = AxisWeightCalculator.compute(command.jobRole(), command.careerYears());
        Map<TestType, AxisAssignment> assignments = AxisWeightCalculator.assignTierAndBudget(weights);

        InterviewSession session = InterviewSession.create(
                command.userId(),
                command.portfolioId(),
                command.jobRole(),
                command.careerYears(),
                command.jdUrl(),
                command.jdText(),
                command.freeText()
        );
        session.assignWeights(weights);
        session = interviewSessionRepository.save(session);

        for (TestType testType : TestType.values()) {
            AxisAssignment assignment = assignments.get(testType);
            interviewAxisPlanRepository.save(
                    InterviewAxisPlan.create(session.getId(), testType, assignment.tier(), assignment.budget())
            );
        }

        ticketHoldUseCase.hold(command.userId(), session.getId());

        registerPreloadTrigger(session.getId());

        return new InterviewSessionCreateResult(session.getId(), session.getStatus());
    }

    private void registerPreloadTrigger(Long sessionId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerPreload(sessionId);
                }
            });
        } else {
            triggerPreload(sessionId);
        }
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
