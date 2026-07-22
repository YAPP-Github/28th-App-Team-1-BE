package com.yapp.d14.feedback.application.command;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestFeedbackSubmitCommandTest {

    private static final String TOKEN = "token";
    private static final String DEVICE_ID = "device-1";

    private List<GuestFeedbackSubmitCommand.RawRating> ratings(int level) {
        return List.of(new GuestFeedbackSubmitCommand.RawRating("GAZE", level, "코멘트"));
    }

    @Test
    void 정상_입력이면_생성한다() {
        GuestFeedbackSubmitCommand command = GuestFeedbackSubmitCommand.of(
                TOKEN, DEVICE_ID, "지인1", ratings(2)
        );

        assertThat(command.ratings()).containsExactly(new GuestFeedbackSubmitCommand.Rating(AttitudeAxis.GAZE, 2, "코멘트"));
    }

    @Test
    void deviceId가_없으면_예외를_던진다() {
        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, null, "지인1", ratings(2)))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.MISSING_DEVICE_ID);
    }

    @Test
    void deviceId가_공백이면_예외를_던진다() {
        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, "  ", "지인1", ratings(2)))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.MISSING_DEVICE_ID);
    }

    @Test
    void ratings가_비어있으면_예외를_던진다() {
        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, "지인1", List.of()))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INCOMPLETE_RATINGS);
    }

    @Test
    void ratings가_null이면_예외를_던진다() {
        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, "지인1", null))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INCOMPLETE_RATINGS);
    }

    @Test
    void level이_범위를_벗어나면_예외를_던진다() {
        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, "지인1", ratings(0)))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INVALID_RATING_LEVEL);

        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, "지인1", ratings(5)))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INVALID_RATING_LEVEL);
    }

    @Test
    void level이_null이면_예외를_던진다() {
        List<GuestFeedbackSubmitCommand.RawRating> raw = List.of(new GuestFeedbackSubmitCommand.RawRating("GAZE", null, null));

        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, "지인1", raw))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INVALID_RATING_LEVEL);
    }

    @Test
    void 같은_axis가_중복되면_예외를_던진다() {
        List<GuestFeedbackSubmitCommand.RawRating> raw = List.of(
                new GuestFeedbackSubmitCommand.RawRating("GAZE", 2, null),
                new GuestFeedbackSubmitCommand.RawRating("GAZE", 3, null)
        );

        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, "지인1", raw))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.DUPLICATE_RATING_AXIS);
    }

    @Test
    void 정의되지_않은_axis면_예외를_던진다() {
        List<GuestFeedbackSubmitCommand.RawRating> raw = List.of(new GuestFeedbackSubmitCommand.RawRating("FOO", 2, null));

        assertThatThrownBy(() -> GuestFeedbackSubmitCommand.of(TOKEN, DEVICE_ID, "지인1", raw))
                .isInstanceOf(FeedbackException.class)
                .extracting(e -> ((FeedbackException) e).getErrorCode())
                .isEqualTo(FeedbackErrorCode.INVALID_ATTITUDE_AXIS);
    }
}
