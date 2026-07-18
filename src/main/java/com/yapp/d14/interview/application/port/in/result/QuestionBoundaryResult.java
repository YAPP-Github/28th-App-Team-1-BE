package com.yapp.d14.interview.application.port.in.result;

public record QuestionBoundaryResult(
        int turnLevel,
        float startSec,
        String questionText
) {
}
