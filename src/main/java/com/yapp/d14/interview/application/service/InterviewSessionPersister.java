package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AxisWeightCalculator.AxisAssignment;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.portfolio.application.port.in.PortfolioLockedStatusCheckUseCase;
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
    private final PortfolioLockedStatusCheckUseCase portfolioLockedStatusCheckUseCase;

    @Transactional
    InterviewSession persist(
            InterviewSessionCreateCommand command,
            InterviewSessionCreateContext context,
            String jdText,
            Map<TestType, Integer> weights,
            Map<TestType, AxisAssignment> assignments
    ) {
        // 검증(validate())과 실제 저장 사이 시간차 동안 포트폴리오가 삭제됐을 수 있으므로,
        // 락을 잡고 마지막으로 한 번 더 확인한다 — PortfolioDeleteService와 같은 락 키로 직렬화된다.
        portfolioLockedStatusCheckUseCase.requireReadyWithLock(command.userId(), command.portfolioId());

        InterviewSession session = InterviewSession.create(
                command.userId(),
                command.portfolioId(),
                context.portfolioFileName(),
                context.jobRole(),
                context.careerYears(),
                command.jdUrl(),
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
