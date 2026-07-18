package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.FeedbackShareQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareStatusResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * F4 — 참여 현황 조회. 이번 스텝은 API 계약 확정 단계라 스텁이다.
 */
@Service
class FeedbackShareQueryService implements FeedbackShareQueryUseCase {

    @Override
    public FeedbackShareStatusResult get(UUID userId, Long sessionId) {
        throw new UnsupportedOperationException("미구현: 다음 스텝(도메인·영속)에서 구현합니다.");
    }
}
