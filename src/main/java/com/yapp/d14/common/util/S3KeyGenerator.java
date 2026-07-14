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
}
