package com.yapp.d14.user.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.user.adapter.in.web.request.TokenReissueHttpRequest;
import com.yapp.d14.user.adapter.in.web.request.UserSocialLoginHttpRequest;
import com.yapp.d14.user.adapter.in.web.response.UserSocialLoginHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yapp.d14.common.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "소셜 로그인",
            description = "카카오 또는 애플 소셜 로그인을 수행합니다.\n\n" +
                    "- **KAKAO**: `credential`에 카카오 액세스 토큰을 전달\n" +
                    "- **APPLE**: `credential`에 애플 authorization code를 전달"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 — JWT 액세스 토큰(3시간) + 리프레시 토큰(7일) 반환",
                    content = @Content(schema = @Schema(implementation = UserSocialLoginHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류 (provider 또는 credential 누락)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "소셜 로그인 실패 (유효하지 않은 토큰)", content = @Content)
    })
    @SecurityRequirements
    ResponseEntity<ApiResponse<UserSocialLoginHttpResponse>> login(@Valid @RequestBody UserSocialLoginHttpRequest request);

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token을 사용해 Access Token과 Refresh Token을 재발급합니다.\n\n" +
                    "- Rotation 방식 적용 — 재발급 시 Refresh Token도 함께 교체됩니다.\n" +
                    "- 기존 Refresh Token은 즉시 만료되므로 반드시 새 토큰으로 교체해야 합니다.\n" +
                    "- Access Token 유효 시간: **3시간** / Refresh Token 유효 시간: **7일**"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공 — 새 Access Token + 새 Refresh Token 반환",
                    content = @Content(schema = @Schema(implementation = UserSocialLoginHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류 (refreshToken 누락)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token", content = @Content)
    })
    @SecurityRequirements
    ResponseEntity<ApiResponse<UserSocialLoginHttpResponse>> reissue(@Valid @RequestBody TokenReissueHttpRequest request);

    @Operation(
            summary = "로그아웃",
            description = "Redis에서 Refresh Token을 삭제합니다.\n\n" +
                    "- Access Token은 만료 시까지 유효하므로 클라이언트에서도 반드시 삭제해야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 성공", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 요청", content = @Content)
    })
    ResponseEntity<Void> logout(@CurrentUser UUID userId);
}
