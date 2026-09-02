package com.yapp.d14.interview.adapter.out.integration.tts;

import com.yapp.d14.common.metrics.AiCallMetrics;
import com.yapp.d14.common.metrics.AiCallStage;
import com.yapp.d14.interview.application.command.AiUsageRecordCommand;
import com.yapp.d14.interview.application.port.in.AiUsageRecordUseCase;
import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechProperties;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
class OpenAiTextToSpeechSynthesizerAdapter implements TextToSpeechSynthesizer {

    // OpenAiAudioSpeechModel은 SpeechModel(동기 call)과 StreamingSpeechModel(stream) 둘 다 구현한다
    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiAudioSpeechProperties speechProperties;
    private final AiUsageRecordUseCase aiUsageRecordUseCase;
    private final AiCallMetrics aiCallMetrics;

    @Override
    public byte[] synthesize(Long sessionId, String text) {
        try {
            return aiCallMetrics.record(AiCallStage.TTS, () -> {
                byte[] audioContent = speechModel.call(text);
                recordUsage(sessionId, text);
                return audioContent;
            });
        } catch (Exception e) {
            log.error("[TTS SYNTHESIZE] OpenAI 호출 실패", e);
            throw new RuntimeException("TTS 합성에 실패했어요.", e);
        }
    }

    @Override
    public Flux<byte[]> synthesizeStream(Long sessionId, String text) {
        return aiCallMetrics.recordStream(AiCallStage.TTS_STREAM, () -> speechModel.stream(text)
                .doOnComplete(() -> recordUsage(sessionId, text))
                .doOnError(e -> log.error("[TTS SYNTHESIZE STREAM] OpenAI 호출 실패", e)));
    }

    private void recordUsage(Long sessionId, String text) {
        if (sessionId == null || text == null) {
            return;
        }
        aiUsageRecordUseCase.record(AiUsageRecordCommand.openAiTts(
                sessionId, speechProperties.getOptions().getModel(), text.length()));
    }
}
