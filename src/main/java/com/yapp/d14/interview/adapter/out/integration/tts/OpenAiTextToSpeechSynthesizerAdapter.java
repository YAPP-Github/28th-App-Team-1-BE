package com.yapp.d14.interview.adapter.out.integration.tts;

import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
class OpenAiTextToSpeechSynthesizerAdapter implements TextToSpeechSynthesizer {

    // OpenAiAudioSpeechModel은 SpeechModel(동기 call)과 StreamingSpeechModel(stream) 둘 다 구현한다
    private final OpenAiAudioSpeechModel speechModel;

    @Override
    public byte[] synthesize(String text) {
        try {
            return speechModel.call(text);
        } catch (Exception e) {
            log.error("[TTS SYNTHESIZE] OpenAI 호출 실패", e);
            throw new RuntimeException("TTS 합성에 실패했어요.", e);
        }
    }

    @Override
    public Flux<byte[]> synthesizeStream(String text) {
        return speechModel.stream(text)
                .doOnError(e -> log.error("[TTS SYNTHESIZE STREAM] OpenAI 호출 실패", e));
    }
}
