package com.yapp.d14.jd.adapter.in.web.response;

import com.yapp.d14.jd.application.port.in.JdCrawlResult;
import com.yapp.d14.jd.application.port.in.JdValidationFailureReason;
import io.swagger.v3.oas.annotations.media.Schema;

public record JdValidateHttpResponse(
        @Schema(description = "JD 크롤링 및 검증 성공 여부") boolean valid,
        @Schema(description = "실패 사유 코드 (성공 시 null)", example = "CRAWLING_FAILED") String reason,
        @Schema(description = "사용자 안내 메시지 (성공 시 null)") String message
) {

    public static JdValidateHttpResponse from(JdCrawlResult result) {
        if (result.isValid()) {
            return new JdValidateHttpResponse(true, null, null);
        }
        JdValidationFailureReason reason = result.getFailureReason();
        return new JdValidateHttpResponse(false, reason.name(), reason.getMessage());
    }
}
