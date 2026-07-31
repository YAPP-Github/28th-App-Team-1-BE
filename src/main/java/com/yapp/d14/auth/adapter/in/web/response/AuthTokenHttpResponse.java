package com.yapp.d14.auth.adapter.in.web.response;

import com.yapp.d14.auth.application.port.in.result.AuthToken;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthTokenHttpResponse(
        @Schema(description = "JWT 액세스 토큰 (유효 시간: 3시간)")
        String accessToken,

        @Schema(description = "리프레시 토큰 (유효 시간: 7일)")
        String refreshToken,

        @Schema(description = "회원 정보(직무·연차·이름) 등록 여부", example = "true")
        boolean profileRegistered,

        @Schema(description = "신규 회원 여부. true면 이번 요청으로 계정이 새로 생성됨", example = "false")
        boolean newUser,

        @Schema(
                description = "필수 동의 상태. NOT_SUBMITTED(온보딩 필요·A1) / STALE(일부 항목 재동의 필요·A3) / " +
                        "UP_TO_DATE(최신 상태·홈). 클라이언트는 newUser와 이 값을 함께 보고 A1/홈/A3을 라우팅합니다.",
                example = "UP_TO_DATE"
        )
        String consentStatus,

        @Schema(description = "회원 정보")
        UserInfoHttpResponse userInfo
) {

    public static AuthTokenHttpResponse from(AuthToken authToken) {
        return new AuthTokenHttpResponse(
                authToken.accessToken(),
                authToken.refreshToken(),
                authToken.profile().profileRegistered(),
                authToken.newUser(),
                authToken.consentStatus().name(),
                UserInfoHttpResponse.from(authToken.profile())
        );
    }
}
