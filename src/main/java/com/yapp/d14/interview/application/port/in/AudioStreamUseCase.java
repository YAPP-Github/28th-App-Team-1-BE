package com.yapp.d14.interview.application.port.in;

import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AudioStreamUseCase {

    Flux<byte[]> stream(UUID userId, Long sessionId, Long questionId);
}
