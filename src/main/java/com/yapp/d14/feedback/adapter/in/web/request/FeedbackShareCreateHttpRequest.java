package com.yapp.d14.feedback.adapter.in.web.request;

import com.yapp.d14.feedback.application.command.FeedbackShareCreateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record FeedbackShareCreateHttpRequest(
        @Schema(
                description = "지인에게 평가받을 태도 항목(1~5개). 기본은 5개 전체.",
                example = "[\"GAZE\", \"EXPRESSION\", \"POSTURE\", \"GESTURE\", \"VOICE\"]",
                allowableValues = {"GAZE", "EXPRESSION", "POSTURE", "GESTURE", "VOICE"}
        )
        @NotEmpty List<String> axes
) {

    public FeedbackShareCreateCommand toCommand(UUID userId, Long sessionId) {
        return FeedbackShareCreateCommand.of(userId, sessionId, axes);
    }
}
