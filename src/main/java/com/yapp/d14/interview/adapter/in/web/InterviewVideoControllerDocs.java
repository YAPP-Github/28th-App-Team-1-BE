package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.request.InterviewVideoCompleteHttpRequest;
import com.yapp.d14.interview.adapter.in.web.response.InterviewVideoExpiryHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewVideoUploadUrlHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Interview Report", description = "면접 보고서 조회·영상 API")
public interface InterviewVideoControllerDocs {

    @Operation(
            summary = "면접 영상 업로드 URL 발급",
            description = "면접 종료 후, 프론트가 녹화한 영상(약 10분 분량)을 S3에 **직접 업로드**할 수 있는 presigned PUT URL을 발급합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 대용량 영상을 앱 서버로 통과시키지 않고, 클라이언트 → S3 직접 업로드하기 위한 엔드포인트입니다.\n" +
                    "- 발급받은 `uploadUrl`로 영상 바이너리를 **PUT** 요청합니다. 이때 `Content-Type` 헤더를 응답의 `contentType` 값과 동일하게 보내야 합니다(서명에 포함되어 있어 다르면 S3가 거부).\n" +
                    "- `uploadUrl`은 `expiresInSeconds` 초 동안만 유효합니다. 만료되면 다시 발급받으세요.\n" +
                    "- 업로드를 마친 뒤에는 반드시 `POST /video/complete`를 호출해 완료를 알려야 재생 URL이 제공됩니다.\n" +
                    "- 저장 위치는 세션마다 하나로 고정되므로, 재발급받아 다시 올리면 기존 영상을 덮어씁니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "uploadUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/sessions/42/recording/raw.mp4?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=600&...",
                                        "contentType": "video/mp4",
                                        "expiresInSeconds": 600
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 존재하지 않거나 본인 소유가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "INTERVIEW_SESSION_NOT_FOUND",
                                      "message": "면접 세션을 찾을 수 없어요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewVideoUploadUrlHttpResponse>> issueUploadUrl(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );

    @Operation(
            summary = "면접 영상 업로드 완료 확정",
            description = "프론트가 presigned URL로 S3 업로드를 끝낸 뒤 호출합니다. 해당 세션 영상을 \"업로드 완료\"로 표시해 리포트 조회 시 재생 URL(`video.url`)이 제공되도록 합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 멱등(idempotent)합니다. 여러 번 호출해도 안전합니다.\n" +
                    "- 실제 업로드(S3 PUT)가 성공한 뒤에만 호출하세요.\n" +
                    "- 바디는 선택입니다. 면접관 마무리 멘트를 재생했다면 그 재생 구간(`wrapUpStartSec`/`wrapUpEndSec`, 녹화 타임라인 기준 초)을 함께 보내면 " +
                    "합성 영상에 마무리 멘트가 얹히고 리포트 대본에도 포함됩니다. 조기 종료 등으로 마무리 멘트가 없으면 바디를 생략하세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "확정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 존재하지 않거나 본인 소유가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "INTERVIEW_SESSION_NOT_FOUND",
                                      "message": "면접 세션을 찾을 수 없어요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<Void>> completeUpload(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @RequestBody(required = false) InterviewVideoCompleteHttpRequest request
    );

    @Operation(
            summary = "면접 영상 만료까지 남은 시간 조회",
            description = "면접 영상이 삭제(만료)되기까지 남은 시간(초)을 반환합니다. 프론트가 카운트다운 UI를 위해 주기적으로 " +
                    "폴링하는 용도입니다(리포트 응답의 `video.expiresAt`과 같은 만료 시각을 초 단위 잔여시간으로 다시 계산해 내려줍니다).\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 이미 만료됐으면 `expiresInSeconds`는 0이고 `expired`는 true입니다.\n" +
                    "- 최대 보관기간은 30일(2,592,000초)입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "expiresInSeconds": 2591480,
                                        "expired": false
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 없거나 본인 소유가 아니면 `INTERVIEW_SESSION_NOT_FOUND`, 세션은 있으나 아직 영상 레코드가 없으면(업로드 완료 전) `INTERVIEW_VIDEO_NOT_FOUND`.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "세션 없음/미소유", value = """
                                            {
                                              "success": false,
                                              "code": "INTERVIEW_SESSION_NOT_FOUND",
                                              "message": "면접 세션을 찾을 수 없어요."
                                            }
                                            """),
                                    @ExampleObject(name = "영상 레코드 없음", value = """
                                            {
                                              "success": false,
                                              "code": "INTERVIEW_VIDEO_NOT_FOUND",
                                              "message": "면접 영상 정보를 찾을 수 없어요."
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewVideoExpiryHttpResponse>> getExpiry(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );
}
