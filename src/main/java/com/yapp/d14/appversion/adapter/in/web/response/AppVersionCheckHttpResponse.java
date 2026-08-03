package com.yapp.d14.appversion.adapter.in.web.response;

import com.yapp.d14.appversion.application.port.in.result.AppVersionCheckResult;
import com.yapp.d14.appversion.domain.UpdateType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 버전 정책 조회 응답")
public record AppVersionCheckHttpResponse(
        @Schema(description = "업데이트 유형", example = "FORCE")
        UpdateType updateType,

        @Schema(description = "스토어 최신 버전", example = "1.4.0")
        String latestVersion,

        @Schema(description = "최소 지원 버전 (이 버전 미만이면 강제 업데이트)", example = "1.3.0")
        String minSupportedVersion,

        @Schema(description = "플랫폼별 스토어 링크", example = "https://apps.apple.com/app/idXXXXXXXXX")
        String storeUrl,

        @Schema(description = "안내창 제목 (updateType이 NONE이면 null)", example = "업데이트가 필요해요", nullable = true)
        String title,

        @Schema(description = "안내창 본문 (updateType이 NONE이면 null)",
                example = "지금 버전에서는 앱을 이용할 수 없어요. 최신 버전으로 업데이트해 주세요.", nullable = true)
        String body
) {

    public static AppVersionCheckHttpResponse from(AppVersionCheckResult result) {
        return new AppVersionCheckHttpResponse(
                result.updateType(),
                result.latestVersion(),
                result.minSupportedVersion(),
                result.storeUrl(),
                result.title(),
                result.body()
        );
    }
}
