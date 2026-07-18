package com.yapp.d14.feedback.application.command;

import com.yapp.d14.feedback.domain.FeedbackShareStatus;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackShareCloseCommandTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long SESSION_ID = 1L;

    @Test
    void PRIVATE는_정상_생성한다() {
        FeedbackShareCloseCommand command = FeedbackShareCloseCommand.of(USER_ID, SESSION_ID, "PRIVATE");

        assertThat(command.targetStatus()).isEqualTo(FeedbackShareStatus.PRIVATE);
    }

    @Test
    void ACTIVE로의_전환은_예외를_던진다() {
        assertThatThrownBy(() -> FeedbackShareCloseCommand.of(USER_ID, SESSION_ID, "ACTIVE"))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INVALID_SHARE_STATUS);
    }

    @Test
    void 정의되지_않은_status는_예외를_던진다() {
        assertThatThrownBy(() -> FeedbackShareCloseCommand.of(USER_ID, SESSION_ID, "FOO"))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INVALID_SHARE_STATUS);
    }
}
