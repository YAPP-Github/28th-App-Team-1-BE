package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.FeedbackShareCloseCommand;
import com.yapp.d14.feedback.application.port.in.FeedbackShareCloseUseCase;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FeedbackShareCloseService implements FeedbackShareCloseUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final FeedbackShareRepository feedbackShareRepository;

    @Override
    @Transactional
    public void close(FeedbackShareCloseCommand command) {
        interviewSessionOwnershipCheckUseCase.requireOwned(command.userId(), command.sessionId());

        FeedbackShare share = feedbackShareRepository.findBySessionId(command.sessionId())
                .orElseThrow(() -> new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_NOT_FOUND));

        // 비공개는 되돌릴 수 없는 종료다. 영상 보관 시점 계산은 바꾸지 않는다(§2-6).
        // status 단일 필드만 갱신 — axes 등 나머지 컬렉션을 불필요하게 다시 쓰지 않는다.
        feedbackShareRepository.markPrivate(share.getId());
    }
}
