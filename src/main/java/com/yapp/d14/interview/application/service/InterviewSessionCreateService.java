package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
class InterviewSessionCreateService implements InterviewSessionCreateUseCase {

    private final TicketAvailabilityCheckUseCase ticketAvailabilityCheckUseCase;
    private final TicketHoldUseCase ticketHoldUseCase;
    private final InterviewSessionCreateValidator interviewSessionCreateValidator;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;

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

        return new InterviewSessionCreateResult(session.getId(), session.getStatus());
    }
}
