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
            String jdText,
            Map<TestType, Integer> weights,
            Map<TestType, AxisAssignment> assignments
    ) {
        // jdUrl은 생성 시점에 jdText로 이미 해석됐으므로 저장하지 않는다.
        // (preload 단계가 jdUrl을 jdText보다 우선 조회하는데, 캐시가 만료되면 이미 해석된 jdText를 무시하게 된다)
        InterviewSession session = InterviewSession.create(
                command.userId(),
                command.portfolioId(),
                command.jobRole(),
                command.careerYears(),
                null,
                jdText,
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
