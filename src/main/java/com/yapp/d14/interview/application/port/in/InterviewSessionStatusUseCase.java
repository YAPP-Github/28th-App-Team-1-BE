package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;

import java.util.UUID;

public interface InterviewSessionStatusUseCase {

    InterviewSessionStatusResult getStatus(UUID userId, Long sessionId);
}
