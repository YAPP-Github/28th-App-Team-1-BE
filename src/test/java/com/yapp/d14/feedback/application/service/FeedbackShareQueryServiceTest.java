package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.result.FeedbackShareStatusResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.domain.FeedbackShareStatus;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackShareQueryServiceTest {

    @Mock
    private InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;

    @Mock
    private InterviewVideoQueryUseCase interviewVideoQueryUseCase;

    @Mock
    private FeedbackShareRepository feedbackShareRepository;

    @Mock
    private GuestFeedbackRepository guestFeedbackRepository;

    @InjectMocks
    private FeedbackShareQueryService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;

    @Test
    void 참여_현황을_조회한다() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        FeedbackShare share = FeedbackShare.of(
                10L, sessionId, "token", List.of(AttitudeAxis.GAZE), FeedbackShareStatus.ACTIVE, createdAt
        );
        given(feedbackShareRepository.findBySessionId(sessionId)).willReturn(Optional.of(share));
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(2L);
        given(interviewVideoQueryUseCase.getStatus(sessionId)).willReturn(new InterviewVideoStatusResult(expiresAt, false));

        FeedbackShareStatusResult result = service.get(userId, sessionId);

        verify(interviewSessionOwnershipCheckUseCase).requireOwned(userId, sessionId);
        assertThat(result.token()).isEqualTo("token");
        assertThat(result.status()).isEqualTo(FeedbackShareStatus.ACTIVE);
        assertThat(result.submittedCount()).isEqualTo(2);
        assertThat(result.videoExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.requestedAt()).isEqualTo(createdAt);
    }

    @Test
    void 링크가_없으면_예외를_던진다() {
        given(feedbackShareRepository.findBySessionId(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, sessionId))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_SHARE_NOT_FOUND);
    }
}
