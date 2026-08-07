package com.yapp.d14.interview.application.port.out;

import java.util.UUID;

public interface InterviewSessionFileCleaner {

    // 세션 프리픽스(users/{userId}/sessions/{sessionId}/) 하위를 모두 지우고 지운 개수를 돌려준다.
    // 삭제에 실패하면 예외를 던져 호출부가 정리 완료로 표시하지 않도록 한다.
    int deleteSessionFiles(UUID userId, Long sessionId);
}
