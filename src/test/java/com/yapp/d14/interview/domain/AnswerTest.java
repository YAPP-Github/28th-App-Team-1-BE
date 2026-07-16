package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerTest {

    private static Answer create(Float answerStartSec, Float answerEndSec) {
        return Answer.create(
                1L, 100L, "STT 변환된 답변", answerStartSec, answerEndSec, 5f,
                false, null, null, null, null, false, false, null
        );
    }

    @Test
    void 답변_구간이_유효하면_생성된다() {
        Answer answer = create(1.5f, 3.2f);

        assertThat(answer.getAnswerStartSec()).isEqualTo(1.5f);
        assertThat(answer.getAnswerEndSec()).isEqualTo(3.2f);
    }

    @Test
    void 시작과_종료가_모두_null이면_생성된다() {
        Answer answer = create(null, null);

        assertThat(answer.getAnswerStartSec()).isNull();
        assertThat(answer.getAnswerEndSec()).isNull();
    }

    @Test
    void 시작이_종료보다_크면_예외가_발생한다() {
        assertThatThrownBy(() -> create(5f, 3f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(floats = {-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void 시작_시간이_음수거나_유한하지_않으면_예외가_발생한다(float invalidStart) {
        assertThatThrownBy(() -> create(invalidStart, 10f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(floats = {-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void 종료_시간이_음수거나_유한하지_않으면_예외가_발생한다(float invalidEnd) {
        assertThatThrownBy(() -> create(0f, invalidEnd))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
