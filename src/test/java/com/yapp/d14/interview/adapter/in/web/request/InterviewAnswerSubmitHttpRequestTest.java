package com.yapp.d14.interview.adapter.in.web.request;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewAnswerSubmitHttpRequestTest {

    private static final Long SESSION_ID = 1L;

    private InterviewAnswerSubmitHttpRequest request(String endType) {
        return new InterviewAnswerSubmitHttpRequest(2L, 100f, 110f, 0f, 5f, 5f, endType, false);
    }

    @Test
    void audio_파일이_있으면_바이트_배열로_변환한다() {
        MultipartFile audio = new MockMultipartFile("audio", "answer.mp3", "audio/mpeg", "content".getBytes());

        InterviewAnswerSubmitCommand command = request(null).toCommand(SESSION_ID, audio);

        assertThat(command.audioContent()).isEqualTo("content".getBytes());
    }

    @Test
    void audio가_null이면_audioContent도_null이다() {
        InterviewAnswerSubmitCommand command = request("SKIP").toCommand(SESSION_ID, null);

        assertThat(command.audioContent()).isNull();
    }

    @Test
    void audio가_빈_파일이면_audioContent는_null이다() {
        MultipartFile emptyAudio = new MockMultipartFile("audio", "answer.mp3", "audio/mpeg", new byte[0]);

        InterviewAnswerSubmitCommand command = request("SKIP").toCommand(SESSION_ID, emptyAudio);

        assertThat(command.audioContent()).isNull();
    }
}
