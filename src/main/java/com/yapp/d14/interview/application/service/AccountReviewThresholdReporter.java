package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

// NETWORK_DISCONNECT 누적이 임계에 닿은 계정을 운영 검토 대상으로 알린다(PRD Part7 계정 정지 정책).
// 자동 정지는 하지 않는다 — 정지 전환은 운영자가 확인 후 직접 처리한다.
@Slf4j
@Component
@RequiredArgsConstructor
class AccountReviewThresholdReporter {

    private static final long REVIEW_THRESHOLD = 15;

    private final InterviewSessionRepository interviewSessionRepository;

    // 임계에 "도달한" 순간에만 알린다. 이후 누적이 더 쌓여도 매번 알리면 운영 검토 신호가 묻힌다.
    void reportIfThresholdReached(UUID userId) {
        long count = interviewSessionRepository.countByUserIdAndAbandonCause(userId, AbandonCause.NETWORK_DISCONNECT);
        if (count != REVIEW_THRESHOLD) {
            return;
        }

        // TODO 알림 서비스 연동 검토중
        log.warn("[ACCOUNT REVIEW] 운영 검토 대상입니다: userId={}, cause={}, count={}",
                userId, AbandonCause.NETWORK_DISCONNECT, count);
    }
}
