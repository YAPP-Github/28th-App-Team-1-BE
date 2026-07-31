package com.yapp.d14.interview.application.port.in.result;

// 폴링용: 영상 만료까지 남은 시간만 담는다(최대 30일 = 2,592,000초). 이미 만료됐으면 expiresInSeconds=0.
public record InterviewVideoExpiryResult(
        long expiresInSeconds,
        boolean expired
) {
}
