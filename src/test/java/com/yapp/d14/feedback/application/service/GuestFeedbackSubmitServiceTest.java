package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackSubmitResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.domain.FeedbackShareStatus;
import com.yapp.d14.feedback.domain.GuestFeedback;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GuestFeedbackSubmitServiceTest {

    @Mock
    private FeedbackShareRepository feedbackShareRepository;

    @Mock
    private GuestFeedbackRepository guestFeedbackRepository;

    @Mock
    private InterviewVideoQueryUseCase interviewVideoQueryUseCase;

    @Mock
    private InterviewVideoRetentionExtendUseCase interviewVideoRetentionExtendUseCase;

    @InjectMocks
    private GuestFeedbackSubmitService service;

    private static final String TOKEN = "token";
    private static final String DEVICE_ID = "device-1";
    private final Long sessionId = 1L;

    private FeedbackShare activeShare(List<AttitudeAxis> axes) {
        return FeedbackShare.of(10L, sessionId, TOKEN, axes, FeedbackShareStatus.ACTIVE, LocalDateTime.now());
    }

    private GuestFeedbackSubmitCommand commandWith(String nickname, List<AttitudeAxis> axes) {
        List<GuestFeedbackSubmitCommand.RawRating> raw = axes.stream()
                .map(axis -> new GuestFeedbackSubmitCommand.RawRating(axis.name(), 2, null))
                .toList();
        return GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, nickname, raw);
    }

    private void stubOpenVideo() {
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().plusDays(1), false));
    }

    @Test
    void 토큰이_없으면_예외를_던진다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(commandWith("지인1", List.of(AttitudeAxis.GAZE))))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_SHARE_TOKEN_NOT_FOUND);
    }

    @Test
    void 비공개_링크면_예외를_던진다() {
        FeedbackShare share = FeedbackShare.of(10L, sessionId, TOKEN, List.of(AttitudeAxis.GAZE), FeedbackShareStatus.PRIVATE, LocalDateTime.now());
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN)).willReturn(Optional.of(share));

        assertThatThrownBy(() -> service.submit(commandWith("지인1", List.of(AttitudeAxis.GAZE))))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_SHARE_CLOSED);

        verify(guestFeedbackRepository, never()).save(any());
    }

    @Test
    void 영상이_만료됐으면_예외를_던진다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN)).willReturn(Optional.of(activeShare(List.of(AttitudeAxis.GAZE))));
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().minusDays(1), true));

        assertThatThrownBy(() -> service.submit(commandWith("지인1", List.of(AttitudeAxis.GAZE))))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_SHARE_CLOSED);
    }

    @Test
    void 동일_기기가_이미_제출했으면_예외를_던진다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN)).willReturn(Optional.of(activeShare(List.of(AttitudeAxis.GAZE))));
        stubOpenVideo();
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(true);

        assertThatThrownBy(() -> service.submit(commandWith("지인1", List.of(AttitudeAxis.GAZE))))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_ALREADY_SUBMITTED);

        verify(guestFeedbackRepository, never()).countBySessionId(any());
    }

    @Test
    void 정원이_찼으면_예외를_던진다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN)).willReturn(Optional.of(activeShare(List.of(AttitudeAxis.GAZE))));
        stubOpenVideo();
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(false);
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(4L);

        assertThatThrownBy(() -> service.submit(commandWith("지인1", List.of(AttitudeAxis.GAZE))))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.FEEDBACK_CAPACITY_FULL);
    }

    @Test
    void 지정된_항목을_다_채우지_못하면_예외를_던진다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN))
                .willReturn(Optional.of(activeShare(List.of(AttitudeAxis.GAZE, AttitudeAxis.VOICE))));
        stubOpenVideo();
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(false);
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(0L);

        assertThatThrownBy(() -> service.submit(commandWith("지인1", List.of(AttitudeAxis.GAZE))))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INCOMPLETE_RATINGS);

        verify(guestFeedbackRepository, never()).save(any());
    }

    @Test
    void 지정된_항목을_초과해도_예외를_던진다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN))
                .willReturn(Optional.of(activeShare(List.of(AttitudeAxis.GAZE))));
        stubOpenVideo();
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(false);
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(0L);

        assertThatThrownBy(() -> service.submit(commandWith("지인1", List.of(AttitudeAxis.GAZE, AttitudeAxis.VOICE))))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INCOMPLETE_RATINGS);
    }

    @Test
    void 정상_제출하면_저장하고_최초_제출_사건으로_영상을_연장한다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN))
                .willReturn(Optional.of(activeShare(List.of(AttitudeAxis.GAZE))));
        stubOpenVideo();
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(false);
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(0L);
        given(guestFeedbackRepository.save(any())).willAnswer(invocation -> {
            GuestFeedback fb = invocation.getArgument(0);
            return GuestFeedback.of(99L, fb.getSessionId(), fb.getNickname(), fb.getDeviceId(), fb.getRatings(), fb.getSubmittedAt());
        });

        GuestFeedbackSubmitResult result = service.submit(commandWith("재원", List.of(AttitudeAxis.GAZE)));

        assertThat(result.submissionId()).isEqualTo(99L);
        verify(interviewVideoRetentionExtendUseCase).extendForGuestFirstSubmitted(sessionId);

        ArgumentCaptor<GuestFeedback> captor = ArgumentCaptor.forClass(GuestFeedback.class);
        verify(guestFeedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("재원");
    }

    @Test
    void 닉네임이_비어있으면_지인N으로_자동_생성한다() {
        given(feedbackShareRepository.findByTokenForUpdate(TOKEN))
                .willReturn(Optional.of(activeShare(List.of(AttitudeAxis.GAZE))));
        stubOpenVideo();
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(false);
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(2L);
        given(guestFeedbackRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.submit(commandWith(null, List.of(AttitudeAxis.GAZE)));

        ArgumentCaptor<GuestFeedback> captor = ArgumentCaptor.forClass(GuestFeedback.class);
        verify(guestFeedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("지인3");
    }
}
