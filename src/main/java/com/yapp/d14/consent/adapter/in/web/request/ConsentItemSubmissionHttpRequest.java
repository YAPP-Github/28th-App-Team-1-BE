package com.yapp.d14.consent.adapter.in.web.request;

import com.yapp.d14.consent.adapter.in.web.ConsentItemParser;
import com.yapp.d14.consent.application.command.ConsentItemSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsentItemSubmissionHttpRequest(
        @Schema(description = "동의 항목 코드", example = "TERMS_OF_SERVICE")
        @NotBlank(message = "동의 항목 코드를 입력해주세요.")
        String item,

        @Schema(description = "동의 대상 버전. GET /api/v1/consents/pending 이 내려준 버전을 그대로 보내야 하며, " +
                "서버의 현행 버전과 다르면 요청이 거부됩니다.", example = "1")
        @NotNull(message = "버전을 입력해주세요.")
        Integer version,

        @Schema(description = "동의 여부. 필수 항목은 false를 보낼 수 없습니다.", example = "true")
        boolean agreed
) {

    public ConsentItemSubmission toSubmission() {
        return new ConsentItemSubmission(ConsentItemParser.parse(item), version, agreed);
    }
}
