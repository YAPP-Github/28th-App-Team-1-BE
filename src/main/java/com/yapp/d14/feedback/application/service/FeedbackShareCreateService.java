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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public FeedbackShareCreateResult create(FeedbackShareCreateCommand command) {
        interviewSessionOwnershipCheckUseCase.requireOwned(command.userId(), command.sessionId());

        if (feedbackShareRepository.findBySessionId(command.sessionId()).isPresent()) {
            throw new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_ALREADY_EXISTS);
        }

        String token = generateToken();
        FeedbackShare share = save(FeedbackShare.create(command.sessionId(), token, command.axes()));

        // 최초 링크 생성이 곧 "피드백 요청" 사건(Step2, +48h)이다.
        interviewVideoRetentionExtendUseCase.extendForShareRequested(command.sessionId());

        return new FeedbackShareCreateResult(share.getToken());
    }

    // 사전 존재 체크(findBySessionId)와 insert 사이의 경쟁 조건 대비 — UNIQUE(session_id) 위반을 비즈니스 예외로 변환한다.
    private FeedbackShare save(FeedbackShare share) {
        try {
            return feedbackShareRepository.save(share);
        } catch (DataIntegrityViolationException e) {
            throw new FeedbackException(FeedbackErrorCode.FEEDBACK_SHARE_ALREADY_EXISTS);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
