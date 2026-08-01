package com.yapp.d14.user.adapter.in.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserNameRegisterHttpRequest(
        @Schema(description = "등록할 이름 (한글·영문만, 최대 5자)", example = "홍길동")
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 5, message = "이름은 5자 이하로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 입력할 수 있어요.")
        String name
) {
}
