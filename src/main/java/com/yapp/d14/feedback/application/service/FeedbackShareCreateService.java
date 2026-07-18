package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.FeedbackShareCreateCommand;
import com.yapp.d14.feedback.application.port.in.FeedbackShareCreateUseCase;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareCreateResult;
import org.springframework.stereotype.Service;

/**
 * F4 — 공유 링크 생성 + 평가 항목 지정. 이번 스텝은 API 계약 확정 단계라 스텁이다.
 * 도메인·영속·영상 수명주기(Step2 연장) 로직은 다음 스텝에서 구현한다.
 */
@Service
class FeedbackShareCreateService implements FeedbackShareCreateUseCase {

    @Override
    public FeedbackShareCreateResult create(FeedbackShareCreateCommand command) {
        throw new UnsupportedOperationException("미구현: 다음 스텝(도메인·영속)에서 구현합니다.");
    }
}
