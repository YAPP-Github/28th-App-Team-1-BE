package com.yapp.d14.feedback.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게스트 진입 시 게이트 판정 결과. 오류가 아니라 화면 분기용 상태라 예외가 아닌 응답 본문으로 내린다.
 * - OPEN: 정상. 영상 시청 + 제출 가능.
 * - PRIVATE: 비공개/무효 링크. "지금은 볼 수 없는 영상" 안내.
 * - EXPIRED: 영상 만료. 시청·평가 불가 안내.
 * - FULL: 정원(4명) 마감. 영상 시청만 가능, 제출 비활성.
 * - ALREADY_SUBMITTED: 이 기기가 이미 제출. "이미 제출하셨어요" 안내.
 */
@Getter
@RequiredArgsConstructor
public enum GuestGate {

    OPEN(true),
    PRIVATE(false),
    EXPIRED(false),
    FULL(false),
    ALREADY_SUBMITTED(false);

    private final boolean submissionOpen;
}
