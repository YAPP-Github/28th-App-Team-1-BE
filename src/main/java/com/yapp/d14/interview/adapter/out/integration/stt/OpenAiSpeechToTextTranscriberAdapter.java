package com.yapp.d14.interview.adapter.out.integration.stt;

import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class OpenAiSpeechToTextTranscriberAdapter implements SpeechToTextTranscriber {

    // 클라이언트 답변 음성 파일은 mp3로 고정 전제 (설계 문서 7-2장). 추후 포맷이 늘어나면 원본 확장자를 전달받아야 한다.
    private static final String AUDIO_FILENAME = "answer.mp3";

    private final OpenAiAudioTranscriptionModel audioTranscriptionModel;

    @Override
    public String transcribe(byte[] audioContent) {
        try {
            Resource audioResource = new ByteArrayResource(audioContent) {
                @Override
                public String getFilename() {
                    return AUDIO_FILENAME;
                }
            };
            return audioTranscriptionModel.call(audioResource);
        } catch (Exception e) {
            log.error("[STT TRANSCRIBE] OpenAI Whisper 호출 실패", e);
            throw new RuntimeException("STT 변환에 실패했어요.", e);
        }
    }
}
