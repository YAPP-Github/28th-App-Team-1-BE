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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

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
            description = "Refresh Token을 사용해 Access Token과 Refresh Token을 재발급합니다. (Rotation 적용)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = UserSocialLoginHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 리프레시 토큰", content = @Content)
    })
    @SecurityRequirements
    ResponseEntity<ApiResponse<UserSocialLoginHttpResponse>> reissue(@Valid @RequestBody TokenReissueHttpRequest request);
}
