package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.InterviewVideo;

import java.util.Optional;

public interface InterviewVideoRepository {

    InterviewVideo save(InterviewVideo interviewVideo);

    Optional<InterviewVideo> findBySessionId(Long sessionId);

    /** 보존 기간 연장(Read-Modify-Write)의 Lost Update 방지용 락 조회. */
    Optional<InterviewVideo> findBySessionIdForUpdate(Long sessionId);

    /** 보관 타이머를 없을 때만 생성한다(있으면 무시). 동시 최초 INSERT 경합을 DB upsert로 흡수한다. */
    void insertRetentionIfAbsent(InterviewVideo interviewVideo);

    /** 업로드 완료로 표시한다. 레코드가 없으면 보관 타이머와 함께 생성(uploaded=true)한다. */
    void upsertUploaded(InterviewVideo interviewVideo);
}
