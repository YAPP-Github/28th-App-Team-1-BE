package com.yapp.d14.interview.application.port.out;

import java.util.UUID;

public interface InterviewSessionFileCleaner {

    int deleteSessionFiles(UUID userId, Long sessionId);
}
