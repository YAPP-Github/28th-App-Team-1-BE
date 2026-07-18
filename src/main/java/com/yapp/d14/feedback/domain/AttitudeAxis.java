package com.yapp.d14.feedback.domain;

import com.yapp.d14.feedback.exception.FeedbackErrorCode;
import com.yapp.d14.feedback.exception.FeedbackException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지인이 평가하는 태도 5종. AI 콘텐츠 축(TestType)과 겹치지 않는 비언어·태도 영역이다.
 * 4단계 척도 문구·질문형 헤드라인은 프론트 카피 책임이며, 서버는 축 코드와 level(1~4)만 보관한다.
 */
@Getter
@RequiredArgsConstructor
public enum AttitudeAxis {

    GAZE("시선"),
    EXPRESSION("표정"),
    POSTURE("자세"),
    GESTURE("손동작"),
    VOICE("목소리");

    public static final int MAX_AXES = 5;

    private final String label;

    public static AttitudeAxis parse(String raw) {
        try {
            return AttitudeAxis.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new FeedbackException(FeedbackErrorCode.INVALID_ATTITUDE_AXIS);
        }
    }
}
