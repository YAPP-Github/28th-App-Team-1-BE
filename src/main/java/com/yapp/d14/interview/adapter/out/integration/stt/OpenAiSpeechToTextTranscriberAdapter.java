package com.yapp.d14.interview.adapter.out.integration.stt;

import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import com.yapp.d14.interview.application.port.out.TranscriptionResult;
import com.yapp.d14.interview.domain.TranscriptSegment;
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

    // 클라이언트(iOS/Android 네이티브) 답변 음성은 m4a로 통일 업로드한다. Whisper는 파일명 확장자로 포맷을
    // 추론하므로 실제 바이트(m4a)와 확장자를 일치시킨다. 제출 API에서 m4a 계열만 허용하므로 여기서는 고정으로 둔다.
    private static final String AUDIO_FILENAME = "answer.m4a";
    // 5-2장: no_speech_prob이 이 값을 초과하는 세그먼트를 인식 실패로 간주
    private static final float NO_SPEECH_PROB_THRESHOLD = 0.6f;

    private final OpenAiAudioApi openAiAudioApi;
    private final OpenAiAudioTranscriptionProperties transcriptionProperties;

    @Override
    public TranscriptionResult transcribe(byte[] audioContent) {
        try {
            // verbose_json은 별도 옵션 없이 발화 세그먼트(start/end/text/no_speech_prob)를 반환한다 —
            // 실패율 계산(no_speech_prob)과 문장 단위 발화 시각 매핑(#78)에 모두 이 세그먼트를 쓴다.
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

            return new TranscriptionResult(body.text(), segments.size(), failedSegmentCount, toSegments(segments));
        } catch (Exception e) {
            log.error("[STT TRANSCRIBE] OpenAI Whisper 호출 실패", e);
            throw new RuntimeException("STT 변환에 실패했어요.", e);
        }
    }

    private boolean isFailedSegment(OpenAiAudioApi.StructuredResponse.Segment segment) {
        return segment.noSpeechProb() != null && segment.noSpeechProb() > NO_SPEECH_PROB_THRESHOLD;
    }

    // Whisper 발화 세그먼트 → 도메인 TranscriptSegment. start/end가 null이면 0으로 보정한다.
    private List<TranscriptSegment> toSegments(List<OpenAiAudioApi.StructuredResponse.Segment> segments) {
        return segments.stream()
                .map(segment -> new TranscriptSegment(
                        segment.text(),
                        segment.start() == null ? 0f : segment.start(),
                        segment.end() == null ? 0f : segment.end()))
                .toList();
    }
}
