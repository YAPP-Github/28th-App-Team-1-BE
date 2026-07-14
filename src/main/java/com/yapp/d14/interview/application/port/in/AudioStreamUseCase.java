package com.yapp.d14.interview.application.port.in;

import reactor.core.publisher.Flux;

import java.util.UUID;

// GET .../questions/{questionId}/audio/stream 전용 - 질문 텍스트를 온디맨드로 TTS 스트리밍한다 (방법 2-1)
public interface AudioStreamUseCase {

    Flux<byte[]> stream(UUID userId, Long sessionId, Long questionId);
}
