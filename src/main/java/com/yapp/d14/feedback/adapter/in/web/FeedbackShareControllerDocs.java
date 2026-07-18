package com.yapp.d14.feedback.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.feedback.adapter.in.web.request.FeedbackShareCreateHttpRequest;
import com.yapp.d14.feedback.adapter.in.web.request.FeedbackShareUpdateHttpRequest;
import com.yapp.d14.feedback.adapter.in.web.response.FeedbackShareCreateHttpResponse;
import com.yapp.d14.feedback.adapter.in.web.response.FeedbackShareStatusHttpResponse;
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

import java.util.UUID;

@Tag(name = "Feedback Share", description = "지인 피드백 공유 설정 API — 사용자측(인증 필요)")
public interface FeedbackShareControllerDocs {

    @Operation(
            summary = "공유 링크 생성 + 평가 항목 지정",
            description = "R1 리포트에서 지인에게 면접 영상을 공유할 링크(토큰)를 생성합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 태도 항목을 1~5개 지정합니다(기본 5개 전체). 지정 항목은 링크에 귀속되며 생성 후 잠깁니다.\n" +
                    "- 최초 생성이 곧 '피드백 요청' 사건이며, 영상 삭제 예정 시각이 +48h로 연장됩니다.\n" +
                    "- 면접당 활성 링크는 1개입니다. 이미 활성 링크가 있으면 409를 반환합니다(재생성은 이번 범위 밖)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = FeedbackShareCreateHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "평가 항목 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "항목 없음", value = """
                                            { "success": false, "code": "EMPTY_ATTITUDE_AXES", "message": "평가 항목을 최소 1개 선택해 주세요." }
                                            """),
                                    @ExampleObject(name = "항목 초과", value = """
                                            { "success": false, "code": "TOO_MANY_ATTITUDE_AXES", "message": "평가 항목은 최대 5개까지 선택할 수 있어요." }
                                            """),
                                    @ExampleObject(name = "지원하지 않는 항목", value = """
                                            { "success": false, "code": "INVALID_ATTITUDE_AXIS", "message": "지원하지 않는 평가 항목이에요." }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 존재하지 않거나 본인 소유가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "success": false, "code": "INTERVIEW_SESSION_NOT_FOUND", "message": "면접 세션을 찾을 수 없어요." }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 활성 공유 링크 존재",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "success": false, "code": "FEEDBACK_SHARE_ALREADY_EXISTS", "message": "이미 피드백 요청 링크가 있어요." }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<FeedbackShareCreateHttpResponse>> create(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody FeedbackShareCreateHttpRequest request
    );

    @Operation(
            summary = "참여 현황 조회",
            description = "공유 링크의 상태와 참여 현황을 조회합니다.\n\n" +
                    "**인증**: Access Token 필요\n\n" +
                    "- `submittedCount`는 제출 인원(참고치, 최대 4)입니다.\n" +
                    "- `videoExpiresAt`은 영상 삭제 예정 시각입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FeedbackShareStatusHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공유 링크 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "success": false, "code": "FEEDBACK_SHARE_NOT_FOUND", "message": "피드백 공유 링크를 찾을 수 없어요." }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<FeedbackShareStatusHttpResponse>> get(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );

    @Operation(
            summary = "비공개 전환",
            description = "공유 링크를 비공개로 전환합니다.\n\n" +
                    "**인증**: Access Token 필요\n\n" +
                    "- 현재는 `PRIVATE` 전환만 지원합니다. 재공개는 없습니다\n" +
                    "- 비공개 후 새 접근은 차단되지만, 이미 제출된 피드백과 영상 삭제 예정 시각은 유지됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "전환 성공(응답 본문 없음)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 상태 전환",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "success": false, "code": "INVALID_SHARE_STATUS", "message": "지원하지 않는 상태 전환이에요." }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공유 링크 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "success": false, "code": "FEEDBACK_SHARE_NOT_FOUND", "message": "피드백 공유 링크를 찾을 수 없어요." }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<Void>> updateStatus(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody FeedbackShareUpdateHttpRequest request
    );
}
