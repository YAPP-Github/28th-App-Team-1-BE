package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;

import java.util.UUID;

public interface InterviewAnswerSubmitUseCase {

    InterviewAnswerSubmitResult submit(UUID userId, InterviewAnswerSubmitCommand command);
}
