package com.yapp.d14.feedback.application.port.in;

import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackEntryResult;

public interface GuestFeedbackEntryUseCase {

    GuestFeedbackEntryResult enter(String token, String deviceId);
}
