package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeResult;

import java.util.UUID;

public interface InterviewSessionResumeQueryUseCase {

    InterviewSessionResumeResult resume(UUID userId, Long sessionId);
}
