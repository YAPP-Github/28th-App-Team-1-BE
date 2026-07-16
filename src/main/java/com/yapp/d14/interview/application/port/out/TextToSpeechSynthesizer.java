package com.yapp.d14.interview.application.port.out;

import reactor.core.publisher.Flux;

public interface TextToSpeechSynthesizer {

    byte[] synthesize(String text);

    // 청크 단위로 온디맨드 스트리밍 생성 (GET .../audio/stream 전용, 전체 생성 완료를 기다리지 않음)
    Flux<byte[]> synthesizeStream(String text);
}
