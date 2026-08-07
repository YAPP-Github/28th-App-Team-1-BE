package com.yapp.d14.interview.application.port.in;

public interface InterviewSessionFileCleanupUseCase {

    // 리포트 없이 끝나 추적 주체가 없는 세션의 S3 잔여물을 정리하고, 정리한 세션 수를 돌려준다.
    int cleanupOrphanFiles();
}
