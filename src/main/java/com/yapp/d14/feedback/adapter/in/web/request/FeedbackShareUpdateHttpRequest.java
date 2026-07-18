package com.yapp.d14.feedback.adapter.in.web.request;

import com.yapp.d14.feedback.application.command.FeedbackShareCloseCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record FeedbackShareUpdateHttpRequest(
        @Schema(
                description = "전환할 상태. 현재는 PRIVATE(비공개, 되돌릴 수 없음)만 지원.",
                example = "PRIVATE",
                allowableValues = {"PRIVATE"}
        )
        @NotBlank String status
) {

    public FeedbackShareCloseCommand toCommand(UUID userId, Long sessionId) {
        return FeedbackShareCloseCommand.of(userId, sessionId, status);
    }
}
