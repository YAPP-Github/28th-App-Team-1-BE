package com.yapp.d14.consent.adapter.in.web.response;

import com.yapp.d14.consent.domain.RequiredConsentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ConsentPendingItemsHttpResponse(
        @Schema(
                description = "필수 동의 상태. NOT_SUBMITTED(온보딩 필요·최초 동의) / STALE(일부 항목 재동의 필요) / " +
                        "UP_TO_DATE(최신 상태·재동의 불필요)",
                example = "STALE"
        )
        String consentStatus,

        @Schema(description = "회원 정보(직무·연차·이름) 등록 여부", example = "true")
        boolean profileRegistered,

        @Schema(description = "지금 동의가 필요한 항목 목록")
        List<ConsentItemHttpResponse> items
) {

    public static ConsentPendingItemsHttpResponse of(
            RequiredConsentStatus consentStatus,
            boolean profileRegistered,
            List<ConsentItemHttpResponse> items
    ) {
        return new ConsentPendingItemsHttpResponse(consentStatus.name(), profileRegistered, items);
    }
}
