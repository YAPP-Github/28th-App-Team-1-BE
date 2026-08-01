package com.yapp.d14.consent.adapter.in.web.response;

import com.yapp.d14.consent.domain.RequiredConsentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ConsentPendingItemsHttpResponse(
        @Schema(
                description = "필수 동의 상태. NOT_SUBMITTED면 최초 동의(신규 온보딩), STALE이면 재동의 상황입니다.",
                example = "STALE"
        )
        String status,

        @Schema(description = "지금 동의가 필요한 항목 목록")
        List<ConsentItemHttpResponse> items
) {

    public static ConsentPendingItemsHttpResponse of(RequiredConsentStatus status, List<ConsentItemHttpResponse> items) {
        return new ConsentPendingItemsHttpResponse(status.name(), items);
    }
}
