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
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import com.yapp.d14.interview.application.port.in.result.QuestionBoundaryResult;
import com.yapp.d14.user.application.port.in.FindUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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

    @Override
    public GuestFeedbackEntryResult enter(String token, String deviceId) {
        FeedbackShare share = feedbackShareRepository.findByToken(token)
                .orElseThrow(() -> new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_TOKEN_NOT_FOUND));

        Long sessionId = share.getSessionId();
        GuestGate gate = determineGate(share, sessionId, deviceId);

        if (gate == GuestGate.PRIVATE || gate == GuestGate.EXPIRED) {
            return new GuestFeedbackEntryResult(gate, null, List.of(), null, List.of());
        }

        if (gate == GuestGate.OPEN) {
            // 최초 지인 조회 시에만 의미가 있지만, extend는 idempotent(더 긴 쪽만 반영)라 매번 호출해도 안전하다.
            interviewVideoRetentionExtendUseCase.extendForGuestFirstViewed(sessionId);
        }

        String requesterName = findUserUseCase.findById(interviewSessionOwnerQueryUseCase.getOwnerUserId(sessionId)).getName();
        List<QuestionBoundaryResult> boundaries = questionBoundaryQueryUseCase.getQuestionBoundaries(sessionId);

        return new GuestFeedbackEntryResult(
                gate,
                requesterName,
                share.getAxes(),
                null, // 영상 파이프라인 연결 전까지 null
                boundaries.stream()
                        .map(b -> new GuestFeedbackEntryResult.QuestionBoundary(b.turnLevel(), b.startSec(), b.questionText()))
                        .toList()
        );
    }

    private GuestGate determineGate(FeedbackShare share, Long sessionId, String deviceId) {
        if (!share.isActive()) {
            return GuestGate.PRIVATE;
        }

        InterviewVideoStatusResult videoStatus = interviewVideoQueryUseCase.getStatus(sessionId);
        if (videoStatus.deleted() || LocalDateTime.now().isAfter(videoStatus.expiresAt())) {
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
