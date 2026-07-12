package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.ticket.application.port.in.TicketAvailabilityCheckUseCase;
import com.yapp.d14.ticket.application.port.in.TicketHoldUseCase;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewSessionCreateServiceTest {

    @Mock
    private TicketAvailabilityCheckUseCase ticketAvailabilityCheckUseCase;

    @Mock
    private TicketHoldUseCase ticketHoldUseCase;

    @Mock
    private InterviewSessionCreateValidator interviewSessionCreateValidator;

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private InterviewAxisPlanRepository interviewAxisPlanRepository;

    @InjectMocks
    private InterviewSessionCreateService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();
    private final InterviewSessionCreateCommand command =
            new InterviewSessionCreateCommand(userId, portfolioId, JobType.BACKEND, 8, null, null, null);

    private void stubSessionSaveWithId(long sessionId) {
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> {
            InterviewSession saved = invocation.getArgument(0);
            return InterviewSession.of(
                    sessionId, saved.getUserId(), saved.getPortfolioId(), saved.getSnapshotJobType(),
                    saved.getSnapshotYearsOfExperience(), saved.getJdUrl(), saved.getJdText(), saved.getFocusProject(),
                    saved.getStatus(), saved.getStartedAt(), saved.getEndedAt(), saved.getEndType(),
                    saved.getWeightDepth(), saved.getWeightBoundary(), saved.getWeightConnection(),
                    saved.getWeightTradeoff(), saved.getWeightConflict(), saved.getWeightResilience()
            );
        });
    }

    @Test
    void 정상_흐름이면_이용권확인_검증_저장_hold_순서로_실행된다() {
        stubSessionSaveWithId(1L);
        given(interviewAxisPlanRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        InterviewSessionCreateResult result = service.create(command);

        assertThat(result.sessionId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(InterviewSessionStatus.PREPARING);

        InOrder inOrder = inOrder(
                ticketAvailabilityCheckUseCase, interviewSessionCreateValidator,
                interviewSessionRepository, interviewAxisPlanRepository, ticketHoldUseCase
        );
        inOrder.verify(ticketAvailabilityCheckUseCase).checkAvailable(userId);
        inOrder.verify(interviewSessionCreateValidator).validate(command);
        inOrder.verify(interviewSessionRepository).save(any());
        inOrder.verify(interviewAxisPlanRepository, times(6)).save(any());
        inOrder.verify(ticketHoldUseCase).hold(userId, 1L);
    }

    @Test
    void 이용권_확인이_실패하면_이후_단계는_실행되지_않는다() {
        doThrow(new TicketException(TicketErrorCode.NO_REMAINING_TICKET))
                .when(ticketAvailabilityCheckUseCase).checkAvailable(userId);

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(TicketException.class);

        verify(interviewSessionCreateValidator, never()).validate(any());
        verify(interviewSessionRepository, never()).save(any());
        verify(ticketHoldUseCase, never()).hold(any(), any());
    }

    @Test
    void 검증이_실패하면_세션_저장은_실행되지_않는다() {
        doThrow(new RuntimeException("검증 실패")).when(interviewSessionCreateValidator).validate(command);

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(RuntimeException.class);

        verify(interviewSessionRepository, never()).save(any());
        verify(interviewAxisPlanRepository, never()).save(any());
        verify(ticketHoldUseCase, never()).hold(any(), any());
    }

    @Test
    void axis_plan은_6개_TestType_모두_문서_예시대로_저장된다() {
        stubSessionSaveWithId(1L);
        given(interviewAxisPlanRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.create(command);

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
    void hold가_실패하면_예외가_전파된다() {
        stubSessionSaveWithId(1L);
        given(interviewAxisPlanRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new TicketException(TicketErrorCode.NO_REMAINING_TICKET))
                .when(ticketHoldUseCase).hold(userId, 1L);

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(TicketException.class);
    }
}
