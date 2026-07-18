package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.FeedbackShareCreateCommand;
import com.yapp.d14.feedback.application.port.in.FeedbackShareCreateUseCase;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareCreateResult;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoRetentionExtendUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
class FeedbackShareCreateService implements FeedbackShareCreateUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 16;

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewVideoRetentionExtendUseCase interviewVideoRetentionExtendUseCase;
    private final FeedbackShareRepository feedbackShareRepository;

    @Override
    public FeedbackShareCreateResult create(FeedbackShareCreateCommand command) {
        interviewSessionOwnershipCheckUseCase.requireOwned(command.userId(), command.sessionId());

        if (feedbackShareRepository.findBySessionId(command.sessionId()).isPresent()) {
            throw new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_ALREADY_EXISTS);
        }

        String token = generateToken();
        FeedbackShare share = feedbackShareRepository.save(
                FeedbackShare.create(command.sessionId(), token, command.axes())
        );

        // 최초 링크 생성이 곧 "피드백 요청" 사건(Step2, +48h)이다.
        interviewVideoRetentionExtendUseCase.extendForShareRequested(command.sessionId());

        return new FeedbackShareCreateResult(share.getToken());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
