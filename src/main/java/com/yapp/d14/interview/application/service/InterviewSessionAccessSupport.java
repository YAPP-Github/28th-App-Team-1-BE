package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class InterviewSessionAccessSupport {

    InterviewSession requireOwned(InterviewSessionRepository interviewSessionRepository, Long sessionId, UUID userId) {
        return interviewSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND));
    }
}
