package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeConfirmResult;

import java.util.UUID;

public interface InterviewSessionResumeConfirmUseCase {

    InterviewSessionResumeConfirmResult confirmResume(UUID userId, Long sessionId);
}
