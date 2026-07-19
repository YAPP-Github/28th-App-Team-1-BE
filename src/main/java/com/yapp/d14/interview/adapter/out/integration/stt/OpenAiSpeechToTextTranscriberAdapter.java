package com.yapp.d14.interview.adapter.out.integration.stt;

import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import com.yapp.d14.interview.application.port.out.TranscriptionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionProperties;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class OpenAiSpeechToTextTranscriberAdapter implements SpeechToTextTranscriber {

    // 클라이언트 답변 음성 파일은 mp3로 고정 전제 (설계 문서 7-2장). 추후 포맷이 늘어나면 원본 확장자를 전달받아야 한다.
    private static final String AUDIO_FILENAME = "answer.mp3";
    // 5-2장: no_speech_prob이 이 값을 초과하는 세그먼트를 인식 실패로 간주
    private static final float NO_SPEECH_PROB_THRESHOLD = 0.6f;

    private final OpenAiAudioApi openAiAudioApi;
    private final OpenAiAudioTranscriptionProperties transcriptionProperties;

    @Override
    public TranscriptionResult transcribe(byte[] audioContent) {
        try {
            OpenAiAudioApi.TranscriptionRequest request = OpenAiAudioApi.TranscriptionRequest.builder()
                    .file(audioContent)
                    .fileName(AUDIO_FILENAME)
                    .model(transcriptionProperties.getOptions().getModel())
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.VERBOSE_JSON)
                    .build();

            ResponseEntity<OpenAiAudioApi.StructuredResponse> response =
                    openAiAudioApi.createTranscription(request, OpenAiAudioApi.StructuredResponse.class);
            OpenAiAudioApi.StructuredResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Whisper 응답 본문이 비어있어요.");
            }

            List<OpenAiAudioApi.StructuredResponse.Segment> segments =
                    body.segments() == null ? List.of() : body.segments();
            int failedSegmentCount = (int) segments.stream()
                    .filter(this::isFailedSegment)
                    .count();

            return new TranscriptionResult(body.text(), segments.size(), failedSegmentCount);
        } catch (Exception e) {
            log.error("[STT TRANSCRIBE] OpenAI Whisper 호출 실패", e);
            throw new RuntimeException("STT 변환에 실패했어요.", e);
        }
    }

    private boolean isFailedSegment(OpenAiAudioApi.StructuredResponse.Segment segment) {
        return segment.noSpeechProb() != null && segment.noSpeechProb() > NO_SPEECH_PROB_THRESHOLD;
    }
}
