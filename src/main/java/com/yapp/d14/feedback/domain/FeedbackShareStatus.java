package com.yapp.d14.feedback.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공유 링크(토큰) 상태. 게이트 판정의 단일 원천이다.
 * - ACTIVE: 활성. 지인이 진입·제출할 수 있다.
 * - INVALIDATED: 무효. 새 링크 생성으로 이전 링크가 자동 무효화된 상태(재생성 흐름은 이번 범위 밖).
 * - PRIVATE: 비공개. 사용자가 되돌릴 수 없이 종료한 상태(재공개 없음).
 */
@Getter
@RequiredArgsConstructor
public enum FeedbackShareStatus {

    ACTIVE("활성"),
    INVALIDATED("무효"),
    PRIVATE("비공개");

    private final String label;
}
