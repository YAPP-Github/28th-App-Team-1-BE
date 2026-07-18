package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 면접 영상 보관 사이클의 연장 사건(Trigger). 삭제 예정 시각은 항상
 * {@code baseAt(1차 레포트 생성 성공 시각) + extension} 중 더 긴 쪽을 적용한다.
 */
@Getter
@RequiredArgsConstructor
public enum VideoRetentionTrigger {

    REPORT_GENERATED(Duration.ofHours(24)),
    SHARE_REQUESTED(Duration.ofHours(48)),
    GUEST_FIRST_VIEWED(Duration.ofDays(7)),
    GUEST_FIRST_SUBMITTED(Duration.ofDays(30));

    private final Duration extension;
}
