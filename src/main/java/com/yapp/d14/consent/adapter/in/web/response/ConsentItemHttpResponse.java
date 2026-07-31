package com.yapp.d14.consent.adapter.in.web.response;

import com.yapp.d14.consent.domain.ConsentItem;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConsentItemHttpResponse(
        @Schema(description = "동의 항목 코드", example = "TERMS_OF_SERVICE")
        String code,

        @Schema(description = "동의 항목 제목", example = "서비스 이용약관")
        String label,

        @Schema(description = "필수 동의 항목 여부", example = "true")
        boolean required,

        @Schema(description = "지금 동의해야 하는 현행 버전. 본문 조회 시 이 버전으로 조회합니다.", example = "2")
        int version,

        @Schema(description = "해당 버전의 본문이 등록되어 있는지 여부. false면 클라이언트는 본문 보기를 숨깁니다.", example = "true")
        boolean hasDocument
) {

    public static ConsentItemHttpResponse from(ConsentItem item, boolean hasDocument) {
        return new ConsentItemHttpResponse(
                item.name(), item.getLabel(), item.isRequired(), item.getCurrentVersion(), hasDocument
        );
    }
}
