package com.yapp.d14.interview.application.port.in;

public interface InterviewVideoRetentionExtendUseCase {

    /** Step2: 최초 공유 링크 생성(피드백 요청) — 삭제 예정 시각을 baseAt+48h까지 연장한다(더 짧으면 무시). */
    void extendForShareRequested(Long sessionId);

    /** Step3: 지인 최초 영상 조회 — baseAt+7일까지 연장한다. */
    void extendForGuestFirstViewed(Long sessionId);

    /** Step4: 지인 1명 피드백 제출 — baseAt+30일까지 연장한다. */
    void extendForGuestFirstSubmitted(Long sessionId);
}
