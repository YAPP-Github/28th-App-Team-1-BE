package com.yapp.d14.interview.adapter.in.web.request;

import com.yapp.d14.interview.application.command.InterviewAbandonCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record InterviewAbandonHttpRequest(
        @Schema(description = "중단 사유 — NETWORK_DISCONNECT(네트워크 끊김) / USER_EXIT(사용자 이탈)", example = "NETWORK_DISCONNECT")
        @NotBlank
        String cause
) {

    public InterviewAbandonCommand toCommand(Long sessionId) {
        return InterviewAbandonCommand.of(sessionId, cause);
    }
}
