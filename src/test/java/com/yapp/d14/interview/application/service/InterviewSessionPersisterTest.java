package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.AxisWeightCalculator;
import com.yapp.d14.interview.domain.AxisWeightCalculator.AxisAssignment;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.ticket.application.port.in.TicketHoldUseCase;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewSessionPersisterTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private InterviewAxisPlanRepository interviewAxisPlanRepository;

    @Mock
    private TicketHoldUseCase ticketHoldUseCase;

    @InjectMocks
    private InterviewSessionPersister persister;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();
    private final String portfolioFileName = "resume.pdf";
    private final InterviewSessionCreateCommand command =
            new InterviewSessionCreateCommand(userId, portfolioId, JobType.BACKEND, 8, null, null, null);

    private void stubSessionSaveWithId(long sessionId) {
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> {
            InterviewSession saved = invocation.getArgument(0);
            return InterviewSession.of(
                    sessionId, saved.getUserId(), saved.getPortfolioId(), saved.getPortfolioFilename(), saved.getSnapshotJobType(),
                    saved.getSnapshotYearsOfExperience(), saved.getJdUrl(), saved.getJdText(), saved.getFocusProject(),
                    saved.getStatus(), saved.getStartedAt(), saved.getEndedAt(), saved.getEndType(),
                    saved.getWeightDepth(), saved.getWeightBoundary(), saved.getWeightConnection(),
                    saved.getWeightTradeoff(), saved.getWeightConflict(), saved.getWeightResilience(),
                    saved.getSttFailedSegmentCount(), saved.getSttTotalSegmentCount()
            );
        });
    }

    private Map<TestType, Integer> weights() {
        return AxisWeightCalculator.compute(command.jobRole(), command.careerYears());
    }

    private Map<TestType, AxisAssignment> assignments(Map<TestType, Integer> weights) {
        return AxisWeightCalculator.assignTierAndBudget(weights);
    }

    @Test
    void axis_plan은_6개_TestType_모두_문서_예시대로_저장된다() {
        stubSessionSaveWithId(1L);
        given(interviewAxisPlanRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        Map<TestType, Integer> weights = weights();
        Map<TestType, AxisAssignment> assignments = assignments(weights);

        persister.persist(command, command.jdText(), portfolioFileName, weights, assignments);

        ArgumentCaptor<InterviewAxisPlan> captor = ArgumentCaptor.forClass(InterviewAxisPlan.class);
        verify(interviewAxisPlanRepository, times(6)).save(captor.capture());
        List<InterviewAxisPlan> plans = captor.getAllValues();

        assertThat(plans).extracting(InterviewAxisPlan::getTestType)
                .containsExactlyInAnyOrder(TestType.values());
        assertThat(plans).filteredOn(p -> p.getTestType() == TestType.TRADEOFF)
                .extracting(InterviewAxisPlan::getTier).containsExactly(AxisTier.CORE);
        assertThat(plans).filteredOn(p -> p.getTestType() == TestType.CONNECTION)
                .extracting(InterviewAxisPlan::getTier).containsExactly(AxisTier.SUPPORT);
        assertThat(plans).filteredOn(p -> p.getTestType() == TestType.RESILIENCE)
                .extracting(InterviewAxisPlan::getTier).containsExactly(AxisTier.SKIP);
    }

    @Test
    void 세션을_저장하고_ID를_반환한다() {
        stubSessionSaveWithId(1L);
        given(interviewAxisPlanRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        Map<TestType, Integer> weights = weights();
        Map<TestType, AxisAssignment> assignments = assignments(weights);

        InterviewSession session = persister.persist(command, command.jdText(), portfolioFileName, weights, assignments);

        assertThat(session.getId()).isEqualTo(1L);
        verify(ticketHoldUseCase).hold(userId, 1L);
    }

    @Test
    void hold가_실패하면_예외가_전파된다() {
        stubSessionSaveWithId(1L);
        given(interviewAxisPlanRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new TicketException(TicketErrorCode.NO_REMAINING_TICKET))
                .when(ticketHoldUseCase).hold(userId, 1L);
        Map<TestType, Integer> weights = weights();
        Map<TestType, AxisAssignment> assignments = assignments(weights);

        assertThatThrownBy(() -> persister.persist(command, command.jdText(), portfolioFileName, weights, assignments))
                .isInstanceOf(TicketException.class);
    }
}
