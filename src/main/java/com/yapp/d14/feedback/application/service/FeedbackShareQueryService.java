package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.FeedbackShareQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareStatusResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class FeedbackShareQueryService implements FeedbackShareQueryUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewVideoQueryUseCase interviewVideoQueryUseCase;
    private final FeedbackShareRepository feedbackShareRepository;
    private final GuestFeedbackRepository guestFeedbackRepository;

    @Override
    public FeedbackShareStatusResult get(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);

        FeedbackShare share = feedbackShareRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_NOT_FOUND));

        int submittedCount = (int) guestFeedbackRepository.countBySessionId(sessionId);
        InterviewVideoStatusResult videoStatus = interviewVideoQueryUseCase.getStatus(sessionId);

        return new FeedbackShareStatusResult(
                share.getToken(),
                share.getStatus(),
                share.getAxes(),
                submittedCount,
                videoStatus.expiresAt(),
                share.getCreatedAt()
        );
    }
}
