package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.InterviewVideo;

import java.util.Optional;

public interface InterviewVideoRepository {

    InterviewVideo save(InterviewVideo interviewVideo);

    Optional<InterviewVideo> findBySessionId(Long sessionId);

    /** 보존 기간 연장(Read-Modify-Write)의 Lost Update 방지용 락 조회. */
    Optional<InterviewVideo> findBySessionIdForUpdate(Long sessionId);
}
