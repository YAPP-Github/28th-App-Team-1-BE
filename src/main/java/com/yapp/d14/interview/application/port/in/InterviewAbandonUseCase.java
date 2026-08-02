package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.command.InterviewAbandonCommand;
import com.yapp.d14.interview.application.port.in.result.InterviewAbandonResult;

import java.util.UUID;

public interface InterviewAbandonUseCase {

    InterviewAbandonResult abandon(UUID userId, InterviewAbandonCommand command);
}
