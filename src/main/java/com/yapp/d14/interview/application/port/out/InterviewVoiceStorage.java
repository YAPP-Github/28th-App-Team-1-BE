package com.yapp.d14.interview.application.port.out;

import java.util.UUID;

public interface InterviewVoiceStorage {

    String upload(UUID userId, Long sessionId, int turnLevel, byte[] audioContent);

    // 리포트 다시듣기용 - 클라이언트에게 스트리밍한 질문 음성을 그대로 S3에 비동기(tee) 저장한다. 실패해도 예외를 던지지 않는다.
    void uploadAsync(UUID userId, Long sessionId, int turnLevel, byte[] audioContent);

    String readBase64(String s3Key);
}
