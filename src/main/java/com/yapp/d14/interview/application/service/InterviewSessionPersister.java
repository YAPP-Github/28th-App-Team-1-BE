package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AxisWeightCalculator.AxisAssignment;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.ticket.application.port.in.TicketHoldUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
class InterviewSessionPersister {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;
    private final TicketHoldUseCase ticketHoldUseCase;

    @Transactional
    InterviewSession persist(
            InterviewSessionCreateCommand command,
            Map<TestType, Integer> weights,
            Map<TestType, AxisAssignment> assignments
    ) {
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

        return session;
    }
}
