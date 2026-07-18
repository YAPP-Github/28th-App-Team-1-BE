package com.yapp.d14.feedback.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.feedback.adapter.in.web.request.GuestFeedbackSubmitHttpRequest;
import com.yapp.d14.feedback.adapter.in.web.response.GuestFeedbackEntryHttpResponse;
import com.yapp.d14.feedback.adapter.in.web.response.GuestFeedbackSubmitHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Guest Feedback", description = "지인 평가 API — 게스트측(무인증, 토큰 기반)")
public interface GuestFeedbackControllerDocs {

    @Operation(
            summary = "링크 진입 / 게이트 판정",
            description = "공유 토큰으로 진입해 영상·지정 항목·질문 경계를 받고, 게이트 상태를 판정합니다.\n\n" +
                    "**인증 없음** — 공유 토큰과 `Device-Id` 헤더로 식별합니다.\n\n" +
                    "- `gate`: OPEN(정상) / PRIVATE(비공개·무효) / EXPIRED(영상 만료) / FULL(정원 4명 마감) / ALREADY_SUBMITTED(이 기기 제출 완료).\n" +
                    "- 최초 지인 조회 시 영상 삭제 예정 시각이 +7일로 연장됩니다.\n" +
                    "- `Device-Id`는 클라이언트가 생성해 로컬에 보관하는 기기 식별 값입니다(중복 제출 방지 용도)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "진입 성공(게이트 상태 포함)",
                    content = @Content(schema = @Schema(implementation = GuestFeedbackEntryHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "유효하지 않은 토큰",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "success": false, "code": "FEEDBACK_SHARE_TOKEN_NOT_FOUND", "message": "유효하지 않은 링크예요." }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<GuestFeedbackEntryHttpResponse>> enter(
            @Parameter(description = "공유 토큰") @PathVariable String token,
            @Parameter(description = "기기 식별 값") @RequestHeader(value = "Device-Id", required = false) String deviceId
    );

    @Operation(
            summary = "지인 제출",
            description = "지정 항목을 모두 4단계 척도로 채워 제출합니다.\n\n" +
                    "**인증 없음** — 공유 토큰과 `Device-Id` 헤더로 식별합니다.\n\n" +
                    "- 지정 항목을 하나라도 빠뜨리면 제출할 수 없습니다. 항목별 코멘트와 전반 피드백은 선택입니다.\n" +
                    "- 제출은 확정이며 수정할 수 없습니다. 동일 기기 재제출은 차단됩니다.\n" +
                    "- 면접당 누적 최대 4명까지 접수합니다. 정원이 차면 409를 반환합니다.\n" +
                    "- 첫 제출 시 영상 삭제 예정 시각이 +30일로 연장됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "제출 성공",
                    content = @Content(schema = @Schema(implementation = GuestFeedbackSubmitHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "제출 값 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "지정 항목 미충족", value = """
                                            { "success": false, "code": "INCOMPLETE_RATINGS", "message": "지정된 항목을 모두 평가해 주세요." }
                                            """),
                                    @ExampleObject(name = "척도 값 오류", value = """
                                            { "success": false, "code": "INVALID_RATING_LEVEL", "message": "척도 값이 올바르지 않아요." }
                                            """),
                                    @ExampleObject(name = "기기 식별 값 누락", value = """
                                            { "success": false, "code": "MISSING_DEVICE_ID", "message": "기기 식별 값이 필요해요." }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "유효하지 않은 토큰",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "success": false, "code": "FEEDBACK_SHARE_TOKEN_NOT_FOUND", "message": "유효하지 않은 링크예요." }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "참여 불가 — 비공개·정원 마감·중복 제출",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "비공개/만료 링크", value = """
                                            { "success": false, "code": "FEEDBACK_SHARE_CLOSED", "message": "지금은 참여할 수 없는 링크예요." }
                                            """),
                                    @ExampleObject(name = "정원 마감", value = """
                                            { "success": false, "code": "FEEDBACK_CAPACITY_FULL", "message": "이미 4분이 참여했어요." }
                                            """),
                                    @ExampleObject(name = "중복 제출", value = """
                                            { "success": false, "code": "FEEDBACK_ALREADY_SUBMITTED", "message": "이미 제출하셨어요." }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<GuestFeedbackSubmitHttpResponse>> submit(
            @Parameter(description = "공유 토큰") @PathVariable String token,
            @Parameter(description = "기기 식별 값") @RequestHeader(value = "Device-Id", required = false) String deviceId,
            @Valid @RequestBody GuestFeedbackSubmitHttpRequest request
    );
}
