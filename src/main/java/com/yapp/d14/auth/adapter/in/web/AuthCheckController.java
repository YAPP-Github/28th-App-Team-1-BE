package com.yapp.d14.auth.adapter.in.web;

import com.yapp.d14.auth.adapter.in.web.response.AuthCheckHttpResponse;
import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
class AuthCheckController {

    @Operation(
            summary = "인증 확인 (테스트용)",
            description = "JWT 인증이 정상 동작하는지 확인합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 Access Token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "토큰 만료", value = """
                                            {
                                              "success": false,
                                              "code": "TOKEN_EXPIRED",
                                              "message": "만료된 토큰입니다."
                                            }
                                            """),
                                    @ExampleObject(name = "유효하지 않은 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_TOKEN",
                                              "message": "유효하지 않은 토큰입니다."
                                            }
                                            """)
                            }
                    )
            )
    })
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<AuthCheckHttpResponse>> check(@Parameter(hidden = true) @CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(new AuthCheckHttpResponse("인증 성공", userId)));
    }
}
