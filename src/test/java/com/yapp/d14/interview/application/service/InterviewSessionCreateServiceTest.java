package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.in.InterviewSessionPreloadUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.ticket.application.port.in.TicketAvailabilityCheckUseCase;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewSessionCreateServiceTest {

    @Mock
    private TicketAvailabilityCheckUseCase ticketAvailabilityCheckUseCase;

    @Mock
    private InterviewSessionCreateValidator interviewSessionCreateValidator;

    @Mock
    private InterviewSessionPersister interviewSessionPersister;

    @Mock
    private InterviewSessionPreloadUseCase interviewSessionPreloadUseCase;

    @Mock
    private InterviewPreloadFailureHandler interviewPreloadFailureHandler;

    @InjectMocks
    private InterviewSessionCreateService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();
    private final InterviewSessionCreateCommand command =
            new InterviewSessionCreateCommand(userId, portfolioId, JobType.BACKEND, 8, null, null, null);

    private InterviewSession sessionWithId(long sessionId) {
        return InterviewSession.of(
                sessionId, userId, portfolioId, JobType.BACKEND, 8, null, null, null,
                InterviewSessionStatus.PREPARING, null, null, null,
                0, 0, 0, 0, 0, 0
        );
    }

    @Test
    void 정상_흐름이면_이용권확인_검증_저장_preload_순서로_실행된다() {
        given(interviewSessionPersister.persist(any(), any(), any())).willReturn(sessionWithId(1L));

        InterviewSessionCreateResult result = service.create(command);

        assertThat(result.sessionId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(InterviewSessionStatus.PREPARING);

        InOrder inOrder = inOrder(
                ticketAvailabilityCheckUseCase, interviewSessionCreateValidator,
                interviewSessionPersister, interviewSessionPreloadUseCase
        );
        inOrder.verify(ticketAvailabilityCheckUseCase).checkAvailable(userId);
        inOrder.verify(interviewSessionCreateValidator).validate(command);
        inOrder.verify(interviewSessionPersister).persist(any(), any(), any());
        inOrder.verify(interviewSessionPreloadUseCase).preload(1L);
    }

    @Test
    void 이용권_확인이_실패하면_이후_단계는_실행되지_않는다() {
        doThrow(new TicketException(TicketErrorCode.NO_REMAINING_TICKET))
                .when(ticketAvailabilityCheckUseCase).checkAvailable(userId);

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(TicketException.class);

        verify(interviewSessionCreateValidator, never()).validate(any());
        verify(interviewSessionPersister, never()).persist(any(), any(), any());
        verify(interviewSessionPreloadUseCase, never()).preload(any());
    }

    @Test
    void 검증이_실패하면_저장은_실행되지_않는다() {
        doThrow(new RuntimeException("검증 실패")).when(interviewSessionCreateValidator).validate(command);

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(RuntimeException.class);

        verify(interviewSessionPersister, never()).persist(any(), any(), any());
        verify(interviewSessionPreloadUseCase, never()).preload(any());
    }

    @Test
    void 저장이_실패하면_예외가_전파되고_preload는_실행되지_않는다() {
        doThrow(new TicketException(TicketErrorCode.NO_REMAINING_TICKET))
                .when(interviewSessionPersister).persist(any(), any(), any());

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(TicketException.class);

        verify(interviewSessionPreloadUseCase, never()).preload(any());
    }
}
