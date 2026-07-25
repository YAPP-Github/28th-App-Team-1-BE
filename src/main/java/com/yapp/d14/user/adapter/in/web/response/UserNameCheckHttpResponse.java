package com.yapp.d14.user.adapter.in.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserNameCheckHttpResponse(
        @Schema(description = "사용 가능 여부", example = "true")
        boolean available
) {
}
