package com.yapp.d14.feedback.application.port.in;

import com.yapp.d14.feedback.application.command.FeedbackShareCreateCommand;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareCreateResult;

public interface FeedbackShareCreateUseCase {

    FeedbackShareCreateResult create(FeedbackShareCreateCommand command);
}
