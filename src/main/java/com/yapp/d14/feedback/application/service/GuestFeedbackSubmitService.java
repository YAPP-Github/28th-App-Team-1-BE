package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackSubmitUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackSubmitResult;
import org.springframework.stereotype.Service;

/**
 * G4 — 지인 제출. 이번 스텝은 API 계약 확정 단계라 스텁이다.
 * 정원·중복·영상 연장(Step4) 로직은 다음 스텝에서 구현한다.
 */
@Service
class GuestFeedbackSubmitService implements GuestFeedbackSubmitUseCase {

    @Override
    public GuestFeedbackSubmitResult submit(GuestFeedbackSubmitCommand command) {
        throw new UnsupportedOperationException("미구현: 다음 스텝(도메인·영속)에서 구현합니다.");
    }
}
