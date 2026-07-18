package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.InterviewVideo;

import java.util.Optional;

public interface InterviewVideoRepository {

    InterviewVideo save(InterviewVideo interviewVideo);

    Optional<InterviewVideo> findBySessionId(Long sessionId);
}
