package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.InterviewSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSessionRepository {

    InterviewSession save(InterviewSession interviewSession);

    Optional<InterviewSession> findById(Long id);

    List<InterviewSession> findAllByUserId(UUID userId);
}
