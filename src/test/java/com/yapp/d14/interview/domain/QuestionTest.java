package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTest {

    private static Question question() {
        return Question.create(1L, "질문 내용", 0, 0, TestType.DEPTH, null, null, false);
    }

    @Test
    void 재생_구간이_유효하면_기록된다() {
        Question question = question();

        question.markPlayed(1.5f, 3.2f);

        assertThat(question.getQuestionStartSec()).isEqualTo(1.5f);
        assertThat(question.getQuestionEndSec()).isEqualTo(3.2f);
    }

    @Test
    void 시작과_종료가_모두_null이면_그대로_기록된다() {
        Question question = question();

        question.markPlayed(null, null);

        assertThat(question.getQuestionStartSec()).isNull();
        assertThat(question.getQuestionEndSec()).isNull();
    }

    @Test
    void 시작이_종료보다_크면_예외가_발생한다() {
        Question question = question();

        assertThatThrownBy(() -> question.markPlayed(5f, 3f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(floats = {-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void 시작_시간이_음수거나_유한하지_않으면_예외가_발생한다(float invalidStart) {
        Question question = question();

        assertThatThrownBy(() -> question.markPlayed(invalidStart, 10f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(floats = {-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void 종료_시간이_음수거나_유한하지_않으면_예외가_발생한다(float invalidEnd) {
        Question question = question();

        assertThatThrownBy(() -> question.markPlayed(0f, invalidEnd))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
