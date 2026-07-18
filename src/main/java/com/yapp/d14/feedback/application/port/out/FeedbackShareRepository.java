package com.yapp.d14.feedback.application.port.out;

import com.yapp.d14.feedback.domain.FeedbackShare;

import java.util.Optional;

public interface FeedbackShareRepository {

    FeedbackShare save(FeedbackShare feedbackShare);

    Optional<FeedbackShare> findBySessionId(Long sessionId);

    Optional<FeedbackShare> findByToken(String token);

    /** 게스트 제출의 정원 초과·중복 기기 경쟁 상태 방지용 락 조회. */
    Optional<FeedbackShare> findByTokenForUpdate(String token);

    /** status만 PRIVATE로 바꾼다. axes 등 나머지 필드를 다시 쓰지 않는 단건 갱신. */
    void markPrivate(Long id);
}
