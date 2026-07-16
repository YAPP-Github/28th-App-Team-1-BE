package com.yapp.d14.interview.application.command;

import com.yapp.d14.interview.domain.InterviewEndType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewAnswerSubmitCommandTest {

    private static final Long SESSION_ID = 1L;
    private static final Long QUESTION_ID = 2L;

    private MultipartFile audioFile() {
        return new MockMultipartFile("audio", "answer.mp3", "audio/mpeg", "content".getBytes());
    }

    private InterviewAnswerSubmitCommand of(MultipartFile audio, String rawEndType, Boolean isWrapUp) {
        return InterviewAnswerSubmitCommand.of(
                SESSION_ID, QUESTION_ID, audio, 100f, 110f, 0f, 5f, 5f, rawEndType, isWrapUp
        );
    }

    @Test
    void endType이_null이고_audio가_있으면_정상_생성한다() {
        InterviewAnswerSubmitCommand command = of(audioFile(), null, false);

        assertThat(command.endType()).isNull();
        assertThat(command.audioContent()).isNotEmpty();
    }

    @Test
    void endType이_null이고_audio가_없으면_예외를_던진다() {
        assertThatThrownBy(() -> of(null, null, false))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_AUDIO_PRESENCE);
    }

    @Test
    void endType이_SKIP이고_audio가_없으면_정상_생성한다() {
        InterviewAnswerSubmitCommand command = of(null, "SKIP", false);

        assertThat(command.endType()).isEqualTo(InterviewEndType.SKIP);
        assertThat(command.audioContent()).isNull();
    }

    @Test
    void endType이_SKIP이고_audio가_있으면_예외를_던진다() {
        assertThatThrownBy(() -> of(audioFile(), "SKIP", false))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_AUDIO_PRESENCE);
    }

    @Test
    void endType이_MANUAL_END이면_audio_유무와_무관하게_정상_생성한다() {
        assertThat(of(audioFile(), "MANUAL_END", false).endType()).isEqualTo(InterviewEndType.MANUAL_END);
        assertThat(of(null, "MANUAL_END", false).endType()).isEqualTo(InterviewEndType.MANUAL_END);
    }

    @Test
    void endType이_HARD_CAP이면_audio_유무와_무관하게_정상_생성한다() {
        assertThat(of(audioFile(), "HARD_CAP", false).endType()).isEqualTo(InterviewEndType.HARD_CAP);
        assertThat(of(null, "HARD_CAP", false).endType()).isEqualTo(InterviewEndType.HARD_CAP);
    }

    @Test
    void endType이_EARLY_EXIT이면_audio_유무와_무관하게_정상_생성한다() {
        assertThat(of(audioFile(), "EARLY_EXIT", false).endType()).isEqualTo(InterviewEndType.EARLY_EXIT);
        assertThat(of(null, "EARLY_EXIT", false).endType()).isEqualTo(InterviewEndType.EARLY_EXIT);
    }

    @Test
    void 정의되지_않은_endType이면_예외를_던진다() {
        assertThatThrownBy(() -> of(audioFile(), "FOO", false))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_END_TYPE);
    }
}
