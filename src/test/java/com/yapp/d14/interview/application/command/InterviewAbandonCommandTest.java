package com.yapp.d14.interview.application.command;

import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewAbandonCommandTest {

    @ParameterizedTest
    @ValueSource(strings = {"NETWORK_DISCONNECT", "USER_EXIT"})
    void 허용된_cause면_정상_생성된다(String rawCause) {
        InterviewAbandonCommand command = InterviewAbandonCommand.of(1L, rawCause);

        assertThat(command.cause()).isEqualTo(AbandonCause.valueOf(rawCause));
    }

    @Test
    void cause가_null이면_INVALID_ABANDON_CAUSE를_던진다() {
        assertThatThrownBy(() -> InterviewAbandonCommand.of(1L, null))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INVALID_ABANDON_CAUSE);
    }

    @Test
    void cause가_정의되지_않은_값이면_INVALID_ABANDON_CAUSE를_던진다() {
        assertThatThrownBy(() -> InterviewAbandonCommand.of(1L, "SOMETHING_ELSE"))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INVALID_ABANDON_CAUSE);
    }

    @Test
    void cause가_HOLD_EXPIRED면_enum_valueOf는_성공하지만_화이트리스트에서_거부된다() {
        assertThatThrownBy(() -> InterviewAbandonCommand.of(1L, "HOLD_EXPIRED"))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INVALID_ABANDON_CAUSE);
    }
}
