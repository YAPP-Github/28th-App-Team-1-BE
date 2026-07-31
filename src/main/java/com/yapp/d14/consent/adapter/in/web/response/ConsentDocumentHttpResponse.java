package com.yapp.d14.consent.adapter.in.web.response;

import com.yapp.d14.consent.domain.ConsentDocument;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConsentDocumentHttpResponse(
        @Schema(description = "동의 항목 코드", example = "TERMS_OF_SERVICE")
        String item,

        @Schema(description = "문서 버전", example = "2")
        int version,

        @Schema(description = "문서 제목", example = "서비스 이용약관")
        String title,

        @Schema(description = "문서 본문 (마크다운)")
        String content
) {

    public static ConsentDocumentHttpResponse from(ConsentDocument document) {
        return new ConsentDocumentHttpResponse(
                document.getItem().name(),
                document.getVersion(),
                document.getTitle(),
                document.getContent()
        );
    }
}
