package com.yapp.d14.interview.application.port.out;

import java.util.UUID;

public interface InterviewVoiceStorage {

    String upload(UUID userId, Long sessionId, int turnLevel, byte[] audioContent);

    String upload(String key, byte[] audioContent);

    void uploadAsync(UUID userId, Long sessionId, int turnLevel, byte[] audioContent);

    String readBase64(String s3Key);
}
