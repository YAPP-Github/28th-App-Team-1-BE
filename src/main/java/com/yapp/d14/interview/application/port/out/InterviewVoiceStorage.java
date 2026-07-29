package com.yapp.d14.interview.application.port.out;

import java.util.UUID;

public interface InterviewVoiceStorage {

    String upload(UUID userId, Long sessionId, int turnLevel, byte[] audioContent);

    String upload(String key, byte[] audioContent);

    void uploadAsync(UUID userId, Long sessionId, int turnLevel, byte[] audioContent);

    /** 면접자 답변 음성을 S3에 비동기 보관한다(리포트 영상 합성용). 실패해도 면접 흐름에는 영향을 주지 않는다. */
    void uploadAnswerAsync(UUID userId, Long sessionId, int turnLevel, byte[] audioContent);

    String readBase64(String s3Key);
}
