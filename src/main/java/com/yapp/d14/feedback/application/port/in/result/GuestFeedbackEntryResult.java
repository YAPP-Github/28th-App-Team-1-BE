package com.yapp.d14.feedback.application.port.in.result;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.GuestGate;

import java.util.List;

public record GuestFeedbackEntryResult(
        GuestGate gate,
        String requesterName,
        List<AttitudeAxis> axes,
        String videoUrl,
        List<QuestionBoundary> questionBoundaries
) {

    public record QuestionBoundary(
            int turnLevel,
            float startAt,
            String questionText
    ) {
    }
}
