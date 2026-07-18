package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.GuestFeedbackEntryUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackEntryResult;
import org.springframework.stereotype.Service;

/**
 * G4 — 게스트 진입 / 게이트 판정(조회). 이번 스텝은 API 계약 확정 단계라 스텁이다.
 * 게이트 판정·영상 연장(Step3) 로직은 다음 스텝에서 구현한다.
 */
@Service
class GuestFeedbackQueryService implements GuestFeedbackEntryUseCase {

    @Override
    public GuestFeedbackEntryResult enter(String token, String deviceId) {
        throw new UnsupportedOperationException("미구현: 다음 스텝(도메인·영속)에서 구현합니다.");
    }
}
