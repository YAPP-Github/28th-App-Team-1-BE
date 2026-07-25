package com.yapp.d14.interview.application.port.in;

import java.util.UUID;

public interface InterviewVideoCompositeUseCase {

    /**
     * 세션 녹화본(raw.mp4)에 질문 TTS 음성을 합성해 final.mp4로 만든다.
     * 녹화본 업로드 완료 후 비동기로 트리거되며, 실패해도 videoUrl은 null로 유지된다(폴백 없음).
     */
    void composite(UUID userId, Long sessionId);
}
