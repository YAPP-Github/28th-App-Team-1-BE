package com.yapp.d14.interview.application.port.in;

import java.util.UUID;

public interface InterviewSessionOwnerQueryUseCase {

    UUID getOwnerUserId(Long sessionId);
}
