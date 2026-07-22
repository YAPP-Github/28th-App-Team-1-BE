package com.yapp.d14.feedback.application.port.in.result;

import java.util.List;

// 지인 한 명 = Guest 하나. 그 지인이 평가한 태도 항목들을 ratings로 묶는다.
public record GuestFeedbackReportView(
        int participantCount,
        List<Guest> guests
) {

    public record Guest(
            String alias,
            List<Rating> ratings
    ) {
    }

    public record Rating(
            String axis,
            int level,
            String comment
    ) {
    }
}
