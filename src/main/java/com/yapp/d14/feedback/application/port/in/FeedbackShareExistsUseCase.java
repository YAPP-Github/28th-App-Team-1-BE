package com.yapp.d14.feedback.application.port.in;

public interface FeedbackShareExistsUseCase {

    /** 마이페이지 레포트 목록의 "지인 피드백 받기" 가능 여부 판단용. */
    boolean existsForSession(Long sessionId);
}
