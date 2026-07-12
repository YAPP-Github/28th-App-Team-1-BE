package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;

public interface InterviewSessionCreateUseCase {

    InterviewSessionCreateResult create(InterviewSessionCreateCommand command);
}
