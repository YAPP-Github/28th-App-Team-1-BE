package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.GuestFeedbackEntryUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackEntryResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.domain.GuestGate;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnerQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import com.yapp.d14.interview.application.port.in.QuestionBoundaryQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoPlaybackResult;
import com.yapp.d14.interview.application.port.in.result.QuestionBoundaryResult;
import com.yapp.d14.user.application.port.in.FindUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
class GuestFeedbackQueryService implements GuestFeedbackEntryUseCase {

    private static final int MAX_GUEST_SLOTS = 4;

    private final FeedbackShareRepository feedbackShareRepository;
    private final GuestFeedbackRepository guestFeedbackRepository;
    private final InterviewSessionOwnerQueryUseCase interviewSessionOwnerQueryUseCase;
    private final InterviewVideoQueryUseCase interviewVideoQueryUseCase;
    private final InterviewVideoRetentionExtendUseCase interviewVideoRetentionExtendUseCase;
    private final QuestionBoundaryQueryUseCase questionBoundaryQueryUseCase;
    private final FindUserUseCase findUserUseCase;

    // 게이트 판정 자체는 순수 조회지만 OPEN 분기에서 영상 보관기간을 연장(쓰기)하므로 트랜잭션으로 묶는다.
    @Override
    @Transactional
    public GuestFeedbackEntryResult enter(String token, String deviceId) {
        FeedbackShare share = feedbackShareRepository.findByToken(token)
                .orElseThrow(() -> new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_TOKEN_NOT_FOUND));

        Long sessionId = share.getSessionId();
        // 비활성 링크는 영상을 조회할 것도 없이 바로 PRIVATE로 단락시킨다(영상 row 유무와 무관하게 응답).
        if (!share.isActive()) {
            return new GuestFeedbackEntryResult(GuestGate.PRIVATE, null, List.of(), null, List.of());
        }

        // 게이트 판정용 만료 여부와 재생 URL을 한 번의 조회로 함께 받는다(영상 row·소유자 중복 조회 방지).
        InterviewVideoPlaybackResult playback = interviewVideoQueryUseCase.getPlayback(sessionId);
        GuestGate gate = resolveActiveGate(sessionId, deviceId, playback.expired());

        if (gate == GuestGate.EXPIRED) {
            return new GuestFeedbackEntryResult(gate, null, List.of(), null, List.of());
        }

        if (gate == GuestGate.OPEN) {
            // 최초 지인 조회 시에만 의미가 있지만, extend는 idempotent(더 긴 쪽만 반영)라 매번 호출해도 안전하다.
            interviewVideoRetentionExtendUseCase.extendForGuestFirstViewed(sessionId);
        }

        String requesterName = findUserUseCase.findById(interviewSessionOwnerQueryUseCase.getOwnerUserId(sessionId)).getName();
        List<QuestionBoundaryResult> boundaries = questionBoundaryQueryUseCase.getQuestionBoundaries(sessionId);

        // 영상은 실제로 시청·피드백하는 OPEN 게이트에서만 노출한다(정원 초과·이미 제출 상태에는 URL을 주지 않는다).
        String videoUrl = gate == GuestGate.OPEN ? playback.playbackUrl() : null;

        return new GuestFeedbackEntryResult(
                gate,
                requesterName,
                share.getAxes(),
                videoUrl,
                boundaries.stream()
                        .map(b -> new GuestFeedbackEntryResult.QuestionBoundary(b.turnLevel(), b.startSec(), b.questionText()))
                        .toList()
        );
    }

    // 활성 링크 전제. 만료 → 중복 제출 → 정원 초과 순으로 판정하고, 어디에도 걸리지 않으면 OPEN이다.
    private GuestGate resolveActiveGate(Long sessionId, String deviceId, boolean videoExpired) {
        if (videoExpired) {
            return GuestGate.EXPIRED;
        }

        if (StringUtils.hasText(deviceId) && guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, deviceId)) {
            return GuestGate.ALREADY_SUBMITTED;
        }

        if (guestFeedbackRepository.countBySessionId(sessionId) >= MAX_GUEST_SLOTS) {
            return GuestGate.FULL;
        }

        return GuestGate.OPEN;
    }
}
