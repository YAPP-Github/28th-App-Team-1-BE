package com.yapp.d14.interview.adapter.out.integration.tts;

import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class OpenAiTextToSpeechSynthesizerAdapter implements TextToSpeechSynthesizer {

    private final SpeechModel speechModel;

    @Override
    public byte[] synthesize(String text) {
        try {
            return speechModel.call(text);
        } catch (Exception e) {
            log.error("[TTS SYNTHESIZE] OpenAI 호출 실패", e);
            throw new RuntimeException("TTS 합성에 실패했어요.", e);
        }
    }
}
