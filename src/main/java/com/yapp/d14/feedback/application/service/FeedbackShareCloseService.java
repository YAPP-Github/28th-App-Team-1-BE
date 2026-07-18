package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.FeedbackShareCloseCommand;
import com.yapp.d14.feedback.application.port.in.FeedbackShareCloseUseCase;
import org.springframework.stereotype.Service;

/**
 * F4 — 비공개(되돌릴 수 없는 종료) 전환. 이번 스텝은 API 계약 확정 단계라 스텁이다.
 */
@Service
class FeedbackShareCloseService implements FeedbackShareCloseUseCase {

    @Override
    public void close(FeedbackShareCloseCommand command) {
        throw new UnsupportedOperationException("미구현: 다음 스텝(도메인·영속)에서 구현합니다.");
    }
}
