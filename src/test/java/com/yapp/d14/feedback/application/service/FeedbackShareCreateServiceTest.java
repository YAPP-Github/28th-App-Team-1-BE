package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.FeedbackShareCreateCommand;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareCreateResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackShareCreateServiceTest {

    @Mock
    private InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;

    @Mock
    private InterviewVideoRetentionExtendUseCase interviewVideoRetentionExtendUseCase;

    @Mock
    private FeedbackShareRepository feedbackShareRepository;

    @InjectMocks
    private FeedbackShareCreateService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;

    private FeedbackShareCreateCommand command() {
        return FeedbackShareCreateCommand.of(userId, sessionId, List.of("GAZE", "VOICE"));
    }

    @Test
    void 링크가_없으면_생성하고_공유_요청_사건으로_영상을_연장한다() {
        given(feedbackShareRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(feedbackShareRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        FeedbackShareCreateResult result = service.create(command());

        assertThat(result.token()).isNotBlank();
        verify(interviewSessionOwnershipCheckUseCase).requireOwned(userId, sessionId);
        verify(interviewVideoRetentionExtendUseCase).extendForShareRequested(sessionId);
    }

    @Test
    void 이미_활성_링크가_있으면_예외를_던지고_저장하지_않는다() {
        FeedbackShare existing = FeedbackShare.create(sessionId, "existing-token", List.of(AttitudeAxis.GAZE));
        given(feedbackShareRepository.findBySessionId(sessionId)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_SHARE_ALREADY_EXISTS);

        verify(feedbackShareRepository, never()).save(any());
        verify(interviewVideoRetentionExtendUseCase, never()).extendForShareRequested(any());
    }

    @Test
    void 사전_체크를_통과해도_동시_생성으로_유니크_제약이_깨지면_같은_비즈니스_예외로_변환한다() {
        given(feedbackShareRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(feedbackShareRepository.save(any())).willThrow(new DataIntegrityViolationException("duplicate session_id"));

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_SHARE_ALREADY_EXISTS);

        verify(interviewVideoRetentionExtendUseCase, never()).extendForShareRequested(any());
    }
}
