package com.yapp.d14.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

// docs/policy/s3-policy.md의 경로 규칙을 코드로 고정한다. 경로를 바꿀 때는 정책 문서를 먼저 갱신한 뒤 여기를 맞춘다.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class S3KeyGenerator {

    public static String portfolioKey(UUID userId, UUID portfolioId) {
        return "users/%s/portfolios/%s.pdf".formatted(userId, portfolioId);
    }

    public static String interviewVoiceKey(UUID userId, Long sessionId, int turnLevel) {
        return "users/%s/sessions/%s/questions/%s.mp3".formatted(userId, sessionId, turnLevel);
    }

    // 프론트 녹화본. 세션당 1개로 고정되며 userId+sessionId로 결정적 계산 가능(s3-policy.md §3.4).
    public static String interviewRecordingKey(UUID userId, Long sessionId) {
        return "users/%s/sessions/%s/recording/raw.mp4".formatted(userId, sessionId);
    }

    public static String wrapUpMessageKey(String variant) {
        return "system/interview/wrapup-messages/%s.mp3".formatted(variant);
    }
}
