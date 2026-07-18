package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackEntryResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.domain.FeedbackShareStatus;
import com.yapp.d14.feedback.domain.GuestGate;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnerQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import com.yapp.d14.interview.application.port.in.QuestionBoundaryQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import com.yapp.d14.interview.application.port.in.result.QuestionBoundaryResult;
import com.yapp.d14.user.application.port.in.FindUserUseCase;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GuestFeedbackQueryServiceTest {

    @Mock
    private FeedbackShareRepository feedbackShareRepository;

    @Mock
    private GuestFeedbackRepository guestFeedbackRepository;

    @Mock
    private InterviewSessionOwnerQueryUseCase interviewSessionOwnerQueryUseCase;

    @Mock
    private InterviewVideoQueryUseCase interviewVideoQueryUseCase;

    @Mock
    private InterviewVideoRetentionExtendUseCase interviewVideoRetentionExtendUseCase;

    @Mock
    private QuestionBoundaryQueryUseCase questionBoundaryQueryUseCase;

    @Mock
    private FindUserUseCase findUserUseCase;

    @InjectMocks
    private GuestFeedbackQueryService service;

    private static final String TOKEN = "token";
    private static final String DEVICE_ID = "device-1";
    private final Long sessionId = 1L;
    private final UUID ownerId = UUID.randomUUID();

    private FeedbackShare activeShare() {
        return FeedbackShare.of(
                10L, sessionId, TOKEN, List.of(AttitudeAxis.GAZE), FeedbackShareStatus.ACTIVE, LocalDateTime.now()
        );
    }

    @Test
    void 비공개_링크는_PRIVATE_게이트를_반환한다() {
        FeedbackShare share = FeedbackShare.of(
                10L, sessionId, TOKEN, List.of(AttitudeAxis.GAZE), FeedbackShareStatus.PRIVATE, LocalDateTime.now()
        );
        given(feedbackShareRepository.findByToken(TOKEN)).willReturn(Optional.of(share));

        GuestFeedbackEntryResult result = service.enter(TOKEN, DEVICE_ID);

        assertThat(result.gate()).isEqualTo(GuestGate.PRIVATE);
        verify(interviewVideoQueryUseCase, never()).getStatus(any());
    }

    @Test
    void 영상이_만료됐으면_EXPIRED_게이트를_반환한다() {
        given(feedbackShareRepository.findByToken(TOKEN)).willReturn(Optional.of(activeShare()));
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().minusDays(1), false));

        GuestFeedbackEntryResult result = service.enter(TOKEN, DEVICE_ID);

        assertThat(result.gate()).isEqualTo(GuestGate.EXPIRED);
    }

    @Test
    void 영상이_삭제됐으면_EXPIRED_게이트를_반환한다() {
        given(feedbackShareRepository.findByToken(TOKEN)).willReturn(Optional.of(activeShare()));
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().plusDays(1), true));

        GuestFeedbackEntryResult result = service.enter(TOKEN, DEVICE_ID);

        assertThat(result.gate()).isEqualTo(GuestGate.EXPIRED);
    }

    @Test
    void 동일_기기가_이미_제출했으면_ALREADY_SUBMITTED_게이트를_반환한다() {
        given(feedbackShareRepository.findByToken(TOKEN)).willReturn(Optional.of(activeShare()));
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().plusDays(1), false));
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(true);
        given(interviewSessionOwnerQueryUseCase.getOwnerUserId(sessionId)).willReturn(ownerId);
        given(findUserUseCase.findById(ownerId)).willReturn(User.create("a@a.com", "재원", Provider.KAKAO, "pid"));
        given(questionBoundaryQueryUseCase.getQuestionBoundaries(sessionId)).willReturn(List.of());

        GuestFeedbackEntryResult result = service.enter(TOKEN, DEVICE_ID);

        assertThat(result.gate()).isEqualTo(GuestGate.ALREADY_SUBMITTED);
        verify(interviewVideoRetentionExtendUseCase, never()).extendForGuestFirstViewed(any());
    }

    @Test
    void 정원이_찼으면_FULL_게이트를_반환한다() {
        given(feedbackShareRepository.findByToken(TOKEN)).willReturn(Optional.of(activeShare()));
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().plusDays(1), false));
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(false);
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(4L);
        given(interviewSessionOwnerQueryUseCase.getOwnerUserId(sessionId)).willReturn(ownerId);
        given(findUserUseCase.findById(ownerId)).willReturn(User.create("a@a.com", "재원", Provider.KAKAO, "pid"));
        given(questionBoundaryQueryUseCase.getQuestionBoundaries(sessionId)).willReturn(List.of());

        GuestFeedbackEntryResult result = service.enter(TOKEN, DEVICE_ID);

        assertThat(result.gate()).isEqualTo(GuestGate.FULL);
        verify(interviewVideoRetentionExtendUseCase, never()).extendForGuestFirstViewed(any());
    }

    @Test
    void 정상이면_OPEN_게이트와_함께_요청자_이름과_질문_경계를_돌려주고_영상_보관을_연장한다() {
        given(feedbackShareRepository.findByToken(TOKEN)).willReturn(Optional.of(activeShare()));
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().plusDays(1), false));
        given(guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, DEVICE_ID)).willReturn(false);
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(1L);
        given(interviewSessionOwnerQueryUseCase.getOwnerUserId(sessionId)).willReturn(ownerId);
        given(findUserUseCase.findById(ownerId)).willReturn(User.create("a@a.com", "재원", Provider.KAKAO, "pid"));
        given(questionBoundaryQueryUseCase.getQuestionBoundaries(sessionId))
                .willReturn(List.of(new QuestionBoundaryResult(1, 12.5f, "질문 내용")));

        GuestFeedbackEntryResult result = service.enter(TOKEN, DEVICE_ID);

        assertThat(result.gate()).isEqualTo(GuestGate.OPEN);
        assertThat(result.requesterName()).isEqualTo("재원");
        assertThat(result.axes()).containsExactly(AttitudeAxis.GAZE);
        assertThat(result.questionBoundaries()).hasSize(1);
        assertThat(result.questionBoundaries().get(0).questionText()).isEqualTo("질문 내용");
        verify(interviewVideoRetentionExtendUseCase).extendForGuestFirstViewed(sessionId);
    }

    @Test
    void deviceId가_없으면_중복_제출_검사를_생략한다() {
        given(feedbackShareRepository.findByToken(TOKEN)).willReturn(Optional.of(activeShare()));
        given(interviewVideoQueryUseCase.getStatus(sessionId))
                .willReturn(new InterviewVideoStatusResult(LocalDateTime.now().plusDays(1), false));
        given(guestFeedbackRepository.countBySessionId(sessionId)).willReturn(0L);
        given(interviewSessionOwnerQueryUseCase.getOwnerUserId(sessionId)).willReturn(ownerId);
        given(findUserUseCase.findById(ownerId)).willReturn(User.create("a@a.com", "재원", Provider.KAKAO, "pid"));
        given(questionBoundaryQueryUseCase.getQuestionBoundaries(sessionId)).willReturn(List.of());

        GuestFeedbackEntryResult result = service.enter(TOKEN, null);

        assertThat(result.gate()).isEqualTo(GuestGate.OPEN);
        verify(guestFeedbackRepository, never()).existsBySessionIdAndDeviceId(any(), any());
    }
}
