package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoStatusResult;

public interface InterviewVideoQueryUseCase {

    InterviewVideoStatusResult getStatus(Long sessionId);
}
