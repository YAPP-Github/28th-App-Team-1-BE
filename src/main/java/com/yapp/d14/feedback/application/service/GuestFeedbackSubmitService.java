package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackSubmitUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackSubmitResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.AttitudeRating;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.domain.GuestFeedback;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class GuestFeedbackSubmitService implements GuestFeedbackSubmitUseCase {

    private static final int MAX_GUEST_SLOTS = 4;

    private final FeedbackShareRepository feedbackShareRepository;
    private final GuestFeedbackRepository guestFeedbackRepository;
    private final InterviewVideoQueryUseCase interviewVideoQueryUseCase;
    private final InterviewVideoRetentionExtendUseCase interviewVideoRetentionExtendUseCase;

    @Override
    @Transactional
    public GuestFeedbackSubmitResult submit(GuestFeedbackSubmitCommand command) {
        // 같은 세션에 대한 동시 제출을 직렬화해 정원 초과·중복 기기 제출을 막는다(TOCTOU 방지).
        FeedbackShare share = feedbackShareRepository.findByTokenForUpdate(command.token())
                .orElseThrow(() -> new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_TOKEN_NOT_FOUND));
        Long sessionId = share.getSessionId();

        requireOpenForSubmission(share, sessionId);

        if (guestFeedbackRepository.existsBySessionIdAndDeviceId(sessionId, command.deviceId())) {
            throw new FeedbackException(FeedbackErrorCode.FEEDBACK_ALREADY_SUBMITTED);
        }

        long existingCount = guestFeedbackRepository.countBySessionId(sessionId);
        if (existingCount >= MAX_GUEST_SLOTS) {
            throw new FeedbackException(FeedbackErrorCode.FEEDBACK_CAPACITY_FULL);
        }

        requireRatingsCoverDesignatedAxes(share, command.ratings());

        List<AttitudeRating> ratings = command.ratings().stream()
                .map(r -> new AttitudeRating(r.axis(), r.level(), r.comment()))
                .toList();
        String nickname = resolveNickname(command.nickname(), existingCount);

        GuestFeedback saved = guestFeedbackRepository.save(
                GuestFeedback.create(sessionId, nickname, command.deviceId(), ratings, command.overallFeedback())
        );

        // 지인 1명 제출 = 최종 레포트 성립(Step4, +30일). 최종 레포트 성립 트리거 자체는 별도 이슈 범위다.
        interviewVideoRetentionExtendUseCase.extendForGuestFirstSubmitted(sessionId);

        return new GuestFeedbackSubmitResult(saved.getId(), saved.getSubmittedAt());
    }

    private void requireOpenForSubmission(FeedbackShare share, Long sessionId) {
        if (!share.isActive()) {
            throw new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_CLOSED);
        }
        InterviewVideoStatusResult videoStatus = interviewVideoQueryUseCase.getStatus(sessionId);
        if (videoStatus.expired()) {
            throw new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_CLOSED);
        }
    }

    private void requireRatingsCoverDesignatedAxes(FeedbackShare share, List<GuestFeedbackSubmitCommand.Rating> ratings) {
        Set<AttitudeAxis> requiredAxes = Set.copyOf(share.getAxes());
        Set<AttitudeAxis> providedAxes = ratings.stream()
                .map(GuestFeedbackSubmitCommand.Rating::axis)
                .collect(Collectors.toSet());
        if (ratings.size() != requiredAxes.size() || !providedAxes.equals(requiredAxes)) {
            throw new FeedbackException(FeedbackErrorCode.INCOMPLETE_RATINGS);
        }
    }

    private String resolveNickname(String rawNickname, long existingCount) {
        if (rawNickname != null && !rawNickname.isBlank()) {
            return rawNickname;
        }
        return "지인" + (existingCount + 1);
    }
}
