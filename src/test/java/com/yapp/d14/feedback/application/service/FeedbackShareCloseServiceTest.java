package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.FeedbackShareCloseCommand;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackShareCloseServiceTest {

    @Mock
    private InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;

    @Mock
    private FeedbackShareRepository feedbackShareRepository;

    @InjectMocks
    private FeedbackShareCloseService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;

    @Test
    void 활성_링크를_비공개로_전환한다() {
        FeedbackShare share = FeedbackShare.of(10L, sessionId, "token", List.of(AttitudeAxis.GAZE), null, null);
        given(feedbackShareRepository.findBySessionId(sessionId)).willReturn(Optional.of(share));

        service.close(FeedbackShareCloseCommand.of(userId, sessionId, "PRIVATE"));

        verify(interviewSessionOwnershipCheckUseCase).requireOwned(userId, sessionId);
        verify(feedbackShareRepository).markPrivate(10L);
    }

    @Test
    void 링크가_없으면_예외를_던진다() {
        given(feedbackShareRepository.findBySessionId(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.close(FeedbackShareCloseCommand.of(userId, sessionId, "PRIVATE")))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_SHARE_NOT_FOUND);
    }
}
