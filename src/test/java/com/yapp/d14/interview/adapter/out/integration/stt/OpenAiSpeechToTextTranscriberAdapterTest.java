package com.yapp.d14.interview.adapter.out.integration.stt;

import com.yapp.d14.interview.application.port.out.TranscriptionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionProperties;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OpenAiSpeechToTextTranscriberAdapterTest {

    @Mock
    private OpenAiAudioApi openAiAudioApi;

    private OpenAiSpeechToTextTranscriberAdapter adapter;

    private static OpenAiAudioTranscriptionProperties properties() {
        OpenAiAudioTranscriptionProperties properties = new OpenAiAudioTranscriptionProperties();
        properties.getOptions().setModel("whisper-1");
        return properties;
    }

    private static OpenAiAudioApi.StructuredResponse.Segment segment(Float noSpeechProb) {
        return new OpenAiAudioApi.StructuredResponse.Segment(
                0, 0, 0f, 1f, "text", List.of(), 0f, -0.1f, 1f, noSpeechProb
        );
    }

    @Test
    void no_speech_prob이_0점6을_초과하는_세그먼트만_실패로_센다() {
        adapter = new OpenAiSpeechToTextTranscriberAdapter(openAiAudioApi, properties());
        OpenAiAudioApi.StructuredResponse response = new OpenAiAudioApi.StructuredResponse(
                "ko", 5f, "전체 텍스트",
                List.of(),
                List.of(segment(0.04f), segment(0.65f), segment(0.61f), segment(0.6f))
        );
        given(openAiAudioApi.createTranscription(any(OpenAiAudioApi.TranscriptionRequest.class), eq(OpenAiAudioApi.StructuredResponse.class)))
                .willReturn(ResponseEntity.ok(response));

        TranscriptionResult result = adapter.transcribe(new byte[]{1, 2, 3});

        assertThat(result.text()).isEqualTo("전체 텍스트");
        assertThat(result.totalSegmentCount()).isEqualTo(4);
        // 0.6은 초과가 아니므로 실패로 세지 않는다 (0.65, 0.61만 실패)
        assertThat(result.failedSegmentCount()).isEqualTo(2);
    }

    @Test
    void 세그먼트가_비어있으면_전체_실패_모두_0이다() {
        adapter = new OpenAiSpeechToTextTranscriberAdapter(openAiAudioApi, properties());
        OpenAiAudioApi.StructuredResponse response = new OpenAiAudioApi.StructuredResponse(
                "ko", 0f, "", List.of(), List.of()
        );
        given(openAiAudioApi.createTranscription(any(OpenAiAudioApi.TranscriptionRequest.class), eq(OpenAiAudioApi.StructuredResponse.class)))
                .willReturn(ResponseEntity.ok(response));

        TranscriptionResult result = adapter.transcribe(new byte[]{1});

        assertThat(result.totalSegmentCount()).isZero();
        assertThat(result.failedSegmentCount()).isZero();
    }
}
