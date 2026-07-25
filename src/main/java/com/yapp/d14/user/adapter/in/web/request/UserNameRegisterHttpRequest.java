package com.yapp.d14.user.adapter.in.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserNameRegisterHttpRequest(
        @Schema(description = "등록할 이름", example = "홍길동")
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(min = 1, max = 20, message = "이름은 1자 이상 20자 이하로 입력해주세요.")
        String name
) {
}
