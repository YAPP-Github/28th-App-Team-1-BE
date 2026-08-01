package com.yapp.d14.user.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.user.adapter.in.web.request.UserNameRegisterHttpRequest;
import com.yapp.d14.user.adapter.in.web.request.UserProfileUpdateHttpRequest;
import com.yapp.d14.user.adapter.in.web.response.UserProfileHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "User", description = "회원 프로필 API")
public interface UserControllerDocs {

    @Operation(
            summary = "이름 등록/변경 (삭제 예정, 프로필 등록/조회 API로 통일)",
            description = "로그인한 사용자의 이름을 등록하거나 변경합니다. 등록 후에도 다시 호출해 이름을 바꿀 수 있습니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류 (이름 누락·5자 초과·한글/영문 외 문자 포함)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "VALIDATION_ERROR",
                                      "message": "이름은 5자 이하로 입력해주세요."
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_NOT_FOUND",
                                      "message": "존재하지 않는 사용자입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<Void>> registerName(
            @Parameter(hidden = true) UUID userId,
            @Valid UserNameRegisterHttpRequest request
    );

    @Operation(
            summary = "회원 프로필 조회",
            description = "이름, 이메일, 소셜 로그인 제공자, 직무, 연차, 잔여 이용권 수를 조회합니다.\n\n" +
                    "이메일은 소셜 계정에서 제공되지 않은 경우 null일 수 있습니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_NOT_FOUND",
                                      "message": "존재하지 않는 사용자입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<UserProfileHttpResponse>> getProfile(@Parameter(hidden = true) UUID userId);

    @Operation(
            summary = "회원 프로필 등록/수정",
            description = "이름·직무·연차를 등록하거나 수정합니다. 온보딩 시 최초 등록과 이후 재수정 모두 이 API를 사용하며, " +
                    "이름은 매 호출마다 필수로 입력해야 합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류 (이름·직군 누락, 이름 5자 초과·한글/영문 외 문자 포함, 연차 범위 위반) 또는 지원하지 않는 직군 값",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "필수값 누락·범위 위반", value = """
                                            {
                                              "success": false,
                                              "code": "VALIDATION_ERROR",
                                              "message": "연차는 10년 이하여야 해요."
                                            }
                                            """),
                                    @ExampleObject(name = "지원하지 않는 직군", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_JOB_ROLE",
                                              "message": "지원하지 않는 직군이에요."
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_NOT_FOUND",
                                      "message": "존재하지 않는 사용자입니다."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(hidden = true) UUID userId,
            @Valid UserProfileUpdateHttpRequest request
    );

    @Operation(
            summary = "회원 탈퇴",
            description = "계정을 즉시 삭제하고 Refresh Token을 무효화합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 계정(users row)만 삭제하며, 포트폴리오·면접 세션/레포트·이용권·지인 피드백 공유 등 연관 데이터는 삭제되지 않습니다.\n" +
                    "- 계정 삭제 전에 카카오 연결끊기(unlink) 또는 애플 토큰 폐기(revoke) API를 호출해 소셜 연동을 함께 해제합니다. " +
                    "이 호출이 실패하면 탈퇴 자체가 실패하며 계정은 삭제되지 않습니다.\n" +
                    "- 애플 계정은 로그인 시 저장된 refresh_token으로 revoke를 호출합니다. 이 기능 배포 이전에 가입해 저장된 토큰이 없는 경우, " +
                    "재로그인 후 다시 탈퇴를 요청해야 합니다.\n" +
                    "- Access Token은 만료 시까지 유효하므로 클라이언트에서도 반드시 삭제해야 합니다.\n" +
                    "- 같은 소셜 계정으로 재가입하면 신규 사용자로 처리됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "탈퇴 성공", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_NOT_FOUND",
                                      "message": "존재하지 않는 사용자입니다."
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "저장된 애플 refresh_token이 없어 소셜 연동 해제를 할 수 없음 (재로그인 필요)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "SOCIAL_RECONNECT_REQUIRED",
                                      "message": "소셜 연동 정보가 없어 탈퇴할 수 없습니다. 다시 로그인한 뒤 탈퇴해주세요."
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "카카오 연결끊기 또는 애플 토큰 폐기 API 호출 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "SOCIAL_UNLINK_FAILED",
                                      "message": "소셜 연동 해제에 실패했습니다. 잠시 후 다시 시도해주세요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<Void> withdraw(@Parameter(hidden = true) UUID userId);
}
