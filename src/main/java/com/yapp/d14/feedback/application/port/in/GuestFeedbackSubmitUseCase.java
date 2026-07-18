package com.yapp.d14.feedback.application.port.in;

import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackSubmitResult;

public interface GuestFeedbackSubmitUseCase {

    GuestFeedbackSubmitResult submit(GuestFeedbackSubmitCommand command);
}
