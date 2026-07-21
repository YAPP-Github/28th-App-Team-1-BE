package com.yapp.d14.user.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.user.adapter.in.web.request.UserNameRegisterHttpRequest;
import com.yapp.d14.user.adapter.in.web.request.UserProfileUpdateHttpRequest;
import com.yapp.d14.user.adapter.in.web.response.UserNameCheckHttpResponse;
import com.yapp.d14.user.adapter.in.web.response.UserProfileHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "User", description = "회원 프로필 API")
public interface UserControllerDocs {

    @Operation(
            summary = "이름 등록/변경",
            description = "로그인한 사용자의 이름을 등록하거나 변경합니다. 등록 후에도 다시 호출해 이름을 바꿀 수 있습니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 사용 중인 이름"
            )
    })
    ResponseEntity<ApiResponse<Void>> registerName(
            @Parameter(hidden = true) UUID userId,
            @Valid UserNameRegisterHttpRequest request
    );

    @Operation(
            summary = "이름 중복 확인",
            description = "이름 사용 가능 여부를 확인합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserNameCheckHttpResponse.class))
            )
    })
    ResponseEntity<ApiResponse<UserNameCheckHttpResponse>> checkName(String name);

    @Operation(
            summary = "회원 프로필 조회",
            description = "이름, 직무, 연차, 잔여 이용권 수를 조회합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileHttpResponse.class))
            )
    })
    ResponseEntity<ApiResponse<UserProfileHttpResponse>> getProfile(@Parameter(hidden = true) UUID userId);

    @Operation(
            summary = "회원 프로필 수정",
            description = "이름(선택)·직무·연차를 수정합니다. 수정된 값은 이후 클라이언트가 새로 생성하는 면접 세션 요청부터 " +
                    "반영되며(클라이언트가 이 조회값으로 프리필), 과거 세션의 스냅샷 값은 바뀌지 않습니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 사용 중인 이름"
            )
    })
    ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(hidden = true) UUID userId,
            @Valid UserProfileUpdateHttpRequest request
    );
}
