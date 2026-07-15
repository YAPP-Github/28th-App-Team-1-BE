package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.request.InterviewAnswerSubmitHttpRequest;
import com.yapp.d14.interview.adapter.in.web.request.InterviewSessionCreateHttpRequest;
import com.yapp.d14.interview.adapter.in.web.response.InterviewAnswerSubmitHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionCreateHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionStatusHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Interview", description = "면접 세션 API")
public interface InterviewControllerDocs {

    @Operation(
            summary = "면접 세션 생성",
            description = "직군·연차·포트폴리오(및 선택적으로 JD·집중 프로젝트 설명)를 받아 면접 세션을 생성합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 이용권 확인 → 입력 검증 → 항목별 가중치·질문 예산 계산까지 동기로 처리한 뒤, `PROCESSING` 상태로 202를 즉시 반환합니다.\n" +
                    "- 질문 후보 풀 생성(Preload) 등 이후 단계는 비동기로 처리되며, `statusUrl`로 상태를 폴링합니다.\n" +
                    "- `portfolioId`로 지정한 포트폴리오는 반드시 `READY` 상태여야 합니다.\n" +
                    "- `jdUrl`과 `jdText`는 상호 배타적입니다. `jdUrl`은 `/api/v1/jd/validate`로 먼저 검증(캐싱)돼 있어야 합니다.\n" +
                    "- `freeText`(집중 프로젝트 설명)를 입력하면 포트폴리오와의 연관성을 임베딩 유사도로 검사합니다.\n" +
                    "- 계정당 이용권(무료 3회)이 소진되면 세션을 생성할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "생성 접수 성공 — PROCESSING 상태로 생성",
                    content = @Content(schema = @Schema(implementation = InterviewSessionCreateHttpResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류 · 입력 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "필수 값 누락", value = """
                                            {
                                              "success": false,
                                              "code": "VALIDATION_ERROR",
                                              "message": "portfolioId: 널이어서는 안됩니다"
                                            }
                                            """),
                                    @ExampleObject(name = "지원하지 않는 직군", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_JOB_ROLE",
                                              "message": "지원하지 않는 직군이에요."
                                            }
                                            """),
                                    @ExampleObject(name = "잘못된 연차", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_CAREER_YEARS",
                                              "message": "연차를 다시 확인해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "JD 미검증", value = """
                                            {
                                              "success": false,
                                              "code": "JD_NOT_VALIDATED",
                                              "message": "JD 링크를 먼저 검증해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "JD 길이 위반", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_JD_LENGTH",
                                              "message": "JD는 200자 이상 3,000자 이하로 입력해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "집중 프로젝트 설명 길이 위반", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_FREETEXT_LENGTH",
                                              "message": "집중 프로젝트 설명은 10자 이상 300자 이하로 입력해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "포트폴리오와 연관성 부족", value = """
                                            {
                                              "success": false,
                                              "code": "FREETEXT_NOT_RELEVANT",
                                              "message": "입력하신 내용이 포트폴리오와 관련이 적어요."
                                            }
                                            """),
                                    @ExampleObject(name = "포트폴리오 처리 중", value = """
                                            {
                                              "success": false,
                                              "code": "PORTFOLIO_PROCESSING",
                                              "message": "포트폴리오를 아직 분석하고 있어요. 잠시 후 다시 시도해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "포트폴리오 처리 실패", value = """
                                            {
                                              "success": false,
                                              "code": "PORTFOLIO_UPLOAD_FAILED",
                                              "message": "포트폴리오 처리에 실패했어요. 다시 업로드해 주세요."
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "남은 이용권 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "NO_REMAINING_TICKET",
                                      "message": "남은 이용권이 없어요."
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "포트폴리오가 존재하지 않거나 본인 소유가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "PORTFOLIO_NOT_FOUND",
                                      "message": "포트폴리오를 찾을 수 없어요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewSessionCreateHttpResponse>> create(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody InterviewSessionCreateHttpRequest request
    );

    @Operation(
            summary = "면접 세션 준비 상태 조회",
            description = "세션 생성 후 백그라운드에서 진행되는 Preload·요약 질문 준비 상태를 조회합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 3~5초 간격으로 폴링합니다.\n" +
                    "- `PROCESSING`: 아직 준비 중이에요. 계속 폴링해 주세요.\n" +
                    "- `READY`: 준비가 끝났어요. `startedAt`과 `summaryQuestion`(요약 질문 TTS)이 함께 내려옵니다.\n" +
                    "- `FAILED`: Preload가 실패했어요. 이용권은 자동으로 환불됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "준비 중", value = """
                                            {
                                              "success": true,
                                              "data": { "status": "PROCESSING", "startedAt": null, "summaryQuestion": null }
                                            }
                                            """),
                                    @ExampleObject(name = "준비 완료", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "READY",
                                                "startedAt": "2026-07-06T10:00:04",
                                                "summaryQuestion": {
                                                  "questionId": 1,
                                                  "ttsAudio": "//uQxAAAAAAAAAAAAAAAAAAAAAAASW5mbwAAAA8...",
                                                  "turn": { "turnLevel": 0, "depthLevel": 0 }
                                                }
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "준비 실패", value = """
                                            {
                                              "success": true,
                                              "data": { "status": "FAILED", "startedAt": null, "summaryQuestion": null }
                                            }
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
                                    {
                                      "success": false,
                                      "code": "INTERVIEW_SESSION_NOT_FOUND",
                                      "message": "면접 세션을 찾을 수 없어요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewSessionStatusHttpResponse>> getStatus(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );

    @Operation(
            summary = "답변 제출 (turnLevel=0, 첫 턴 전용)",
            description = "요약 질문(turnLevel=0)에 대한 답변을 제출하고 다음 질문을 받습니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 현재는 turnLevel=0(첫 턴) 경로만 지원합니다. turnLevel≥1 일반 매 턴 처리는 추후 지원 예정입니다.\n" +
                    "- 응답에는 오디오가 동봉되지 않습니다. `nextQuestion.questionId`로 " +
                    "`GET /{sessionId}/questions/{questionId}/audio/stream`을 호출해 오디오를 받으세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "제출 성공 — 다음 질문 메타데이터 반환",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "answerId": 12,
                                        "nextQuestion": {
                                          "questionId": 13,
                                          "isLast": false,
                                          "turn": { "turnLevel": 1, "depthLevel": 0 }
                                        },
                                        "wrapUpMessage": null,
                                        "reportId": null
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "아직 지원하지 않는 turnLevel",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "UNSUPPORTED_TURN_LEVEL",
                                      "message": "아직 지원하지 않는 turnLevel이에요."
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션 또는 질문을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "세션 없음", value = """
                                            {
                                              "success": false,
                                              "code": "INTERVIEW_SESSION_NOT_FOUND",
                                              "message": "면접 세션을 찾을 수 없어요."
                                            }
                                            """),
                                    @ExampleObject(name = "질문 없음", value = """
                                            {
                                              "success": false,
                                              "code": "QUESTION_NOT_FOUND",
                                              "message": "질문을 찾을 수 없어요."
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewAnswerSubmitHttpResponse>> submitAnswer(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @Parameter(description = "답변 음성 파일(mp3)") MultipartFile audio,
            @Valid @ParameterObject InterviewAnswerSubmitHttpRequest request
    );
}
