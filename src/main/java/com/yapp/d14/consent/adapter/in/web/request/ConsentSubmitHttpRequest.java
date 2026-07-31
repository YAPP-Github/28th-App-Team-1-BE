package com.yapp.d14.consent.adapter.in.web.request;

import com.yapp.d14.consent.application.command.ConsentSubmitCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ConsentSubmitHttpRequest(
        @Schema(description = "제출할 항목 목록. 최초 제출 시에는 필수 5종을 모두 포함해야 하고, " +
                "재동의 제출 시에는 GET /api/v1/consents/pending 이 내려준 항목만 보내면 됩니다.")
        @NotEmpty(message = "동의 항목을 1개 이상 입력해주세요.")
        @Valid
        List<ConsentItemSubmissionHttpRequest> items
) {

    public ConsentSubmitCommand toCommand(UUID userId) {
        return new ConsentSubmitCommand(
                userId,
                items.stream().map(ConsentItemSubmissionHttpRequest::toSubmission).toList()
        );
    }
}
