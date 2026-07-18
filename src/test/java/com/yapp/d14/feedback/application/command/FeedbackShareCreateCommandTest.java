package com.yapp.d14.feedback.application.command;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackShareCreateCommandTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long SESSION_ID = 1L;

    @Test
    void axes를_그대로_파싱해서_생성한다() {
        FeedbackShareCreateCommand command = FeedbackShareCreateCommand.of(
                USER_ID, SESSION_ID, List.of("GAZE", "VOICE")
        );

        assertThat(command.axes()).containsExactly(AttitudeAxis.GAZE, AttitudeAxis.VOICE);
    }

    @Test
    void 중복된_axes는_제거한다() {
        FeedbackShareCreateCommand command = FeedbackShareCreateCommand.of(
                USER_ID, SESSION_ID, List.of("GAZE", "GAZE", "VOICE")
        );

        assertThat(command.axes()).containsExactly(AttitudeAxis.GAZE, AttitudeAxis.VOICE);
    }

    @Test
    void axes가_비어있으면_예외를_던진다() {
        assertThatThrownBy(() -> FeedbackShareCreateCommand.of(USER_ID, SESSION_ID, List.of()))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.EMPTY_ATTITUDE_AXES);
    }

    @Test
    void axes가_null이면_예외를_던진다() {
        assertThatThrownBy(() -> FeedbackShareCreateCommand.of(USER_ID, SESSION_ID, null))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.EMPTY_ATTITUDE_AXES);
    }

    @Test
    void axes_5개_전체를_지정하면_정상_생성한다() {
        FeedbackShareCreateCommand command = FeedbackShareCreateCommand.of(
                USER_ID, SESSION_ID, List.of("GAZE", "EXPRESSION", "POSTURE", "GESTURE", "VOICE")
        );

        assertThat(command.axes()).hasSize(AttitudeAxis.MAX_AXES);
    }

    @Test
    void 정의되지_않은_axis가_있으면_예외를_던진다() {
        assertThatThrownBy(() -> FeedbackShareCreateCommand.of(USER_ID, SESSION_ID, List.of("FOO")))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INVALID_ATTITUDE_AXIS);
    }
}
