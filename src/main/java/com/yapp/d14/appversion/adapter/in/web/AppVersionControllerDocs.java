package com.yapp.d14.appversion.adapter.in.web;

import com.yapp.d14.appversion.adapter.in.web.response.AppVersionCheckHttpResponse;
import com.yapp.d14.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "AppVersion", description = "앱 버전 정책 API")
public interface AppVersionControllerDocs {

    @Operation(
            summary = "앱 버전 정책 조회",
            description = """
                    앱 실행(스플래시) 시점에 클라이언트의 플랫폼·앱 버전을 전달하면 강제/권장/최신 여부를 판정해 내려줍니다.

                    - `platform`: `IOS` 또는 `ANDROID` (대소문자 무시)
                    - `version`: 스토어 마케팅 버전(`x.x.x`). 자리수가 달라도(`1.2`, `1.2.0.3`) 자리별 정수로 방어적 비교합니다.

                    **판정 규칙**
                    - 현재 버전 &lt; 최소 지원 버전 → `FORCE`
                    - 최소 지원 버전 ≤ 현재 버전 &lt; 최신 버전 → `OPTIONAL`
                    - 현재 버전 ≥ 최신 버전 → `NONE`

                    **인증**: 불필요 (로그인 전 스플래시에서 호출)"""
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = AppVersionCheckHttpResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "FORCE - 강제 업데이트",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "data": {
                                                        "updateType": "FORCE",
                                                        "latestVersion": "1.4.0",
                                                        "minSupportedVersion": "1.3.0",
                                                        "storeUrl": "https://apps.apple.com/app/idXXXXXXXXX",
                                                        "title": "업데이트가 필요해요",
                                                        "body": "지금 버전에서는 앱을 이용할 수 없어요. 최신 버전으로 업데이트해 주세요."
                                                      }
                                                    }"""
                                    ),
                                    @ExampleObject(
                                            name = "OPTIONAL - 권장 업데이트",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "data": {
                                                        "updateType": "OPTIONAL",
                                                        "latestVersion": "1.4.0",
                                                        "minSupportedVersion": "1.3.0",
                                                        "storeUrl": "https://apps.apple.com/app/idXXXXXXXXX",
                                                        "title": "새 버전이 나왔어요",
                                                        "body": "면접 연습 화면이 더 빨라졌어요. 지금 업데이트할까요?"
                                                      }
                                                    }"""
                                    ),
                                    @ExampleObject(
                                            name = "NONE - 최신",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "data": {
                                                        "updateType": "NONE",
                                                        "latestVersion": "1.4.0",
                                                        "minSupportedVersion": "1.3.0",
                                                        "storeUrl": "https://apps.apple.com/app/idXXXXXXXXX",
                                                        "title": null,
                                                        "body": null
                                                      }
                                                    }"""
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 플랫폼(INVALID_PLATFORM) 또는 잘못된 버전 형식(INVALID_VERSION_FORMAT)",
                    content = @Content(schema = @Schema())
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 플랫폼의 버전 정책 미존재(APP_VERSION_POLICY_NOT_FOUND)",
                    content = @Content(schema = @Schema())
            )
    })
    ResponseEntity<ApiResponse<AppVersionCheckHttpResponse>> check(
            @Parameter(description = "플랫폼", example = "IOS", required = true) String platform,
            @Parameter(description = "앱 마케팅 버전", example = "1.2.0", required = true) String version
    );
}
