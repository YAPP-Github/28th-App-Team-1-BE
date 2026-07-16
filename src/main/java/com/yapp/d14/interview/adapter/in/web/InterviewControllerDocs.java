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
            summary = "답변 제출",
            description = "질문에 대한 답변을 제출하고 다음 질문(또는 세션 종료 결과)을 받습니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- turnLevel=0(요약 질문) 응답은 항상 다음 질문을 생성합니다.\n" +
                    "- turnLevel≥1에서는 `endType`에 따라 즉시 세션이 종료될 수 있습니다.\n" +
                    "  - `endType=EARLY_EXIT`: 0:00~8:00 사이 사용자가 의도적으로 이탈 — audio가 있으면 STT만 기록하고 즉시 종료(wrapUpMessage 없음).\n" +
                    "  - `endType=MANUAL_END`: 8:00 이후 수동 종료 — 즉시 종료하며 짧은 마무리 멘트를 반환합니다.\n" +
                    "  - `endType=HARD_CAP`: 12:00 경과 강제 종료 — audio 유무와 무관하게 즉시 종료합니다.\n" +
                    "  - 직전에 받은 질문이 마무리(wrap-up) 질문이었던 경우, endType 없이도 자연 종료됩니다.\n" +
                    "  - 위 종료 경로에서는 `nextQuestion`이 `null`, `sessionEnded`가 `true`이며, 이용권이 확정(commit)되고 리포트 생성이 비동기로 트리거됩니다.\n" +
                    "  - 그 외에는 매 턴 루프로 이어집니다(현재 구현 중), `sessionEnded`는 `false`입니다.\n" +
                    "- `wrapUpMessage.ttsAudio`는 마무리 멘트 음성을 base64로 인코딩한 mp3입니다(EARLY_EXIT은 `wrapUpMessage` 자체가 `null`). " +
                    "고정 문구 3종(MANUAL_END/HARD_CAP/자연종료)은 최초 요청 시 TTS로 합성해 S3에 캐시하고 이후에는 캐시를 재사용합니다.\n" +
                    "- `isWrapUp`은 클라이언트 타이머 기준 8:45 경과 여부이며, 다음 질문을 마무리 질문으로 만들지 여부에 사용됩니다.\n" +
                    "- `audio` 파트는 선택적입니다. `endType=SKIP`이면 audio가 없어야 하고, `endType=null`이면 audio가 있어야 합니다. " +
                    "`MANUAL_END`/`HARD_CAP`/`EARLY_EXIT`은 audio 유무와 무관합니다.\n" +
                    "- 응답에는 오디오가 동봉되지 않습니다. `nextQuestion.questionId`로 " +
                    "`GET /{sessionId}/questions/{questionId}/audio/stream`을 호출해 오디오를 받으세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "제출 성공 — 다음 질문 또는 세션 종료 결과 반환",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "다음 질문 반환", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "answerId": 12,
                                                "nextQuestion": {
                                                  "questionId": 13,
                                                  "isLast": false,
                                                  "turn": { "turnLevel": 1, "depthLevel": 1 }
                                                },
                                                "sessionEnded": false,
                                                "wrapUpMessage": null,
                                                "reportId": null
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "세션 종료(마무리 멘트 음성 포함)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "answerId": 12,
                                                "nextQuestion": null,
                                                "sessionEnded": true,
                                                "wrapUpMessage": {
                                                  "ttsAudio": "base64로 인코딩된 mp3"
                                                },
                                                "reportId": null
                                              }
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "재생·답변 구간 값 오류 또는 endType 관련 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "질문 재생 구간 값 오류", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_PLAYBACK_RANGE",
                                              "message": "질문 재생 구간 값이 올바르지 않아요."
                                            }
                                            """),
                                    @ExampleObject(name = "답변 구간 값 오류", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_ANSWER_RANGE",
                                              "message": "답변 구간 값이 올바르지 않아요."
                                            }
                                            """),
                                    @ExampleObject(name = "지원하지 않는 endType", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_END_TYPE",
                                              "message": "지원하지 않는 endType이에요."
                                            }
                                            """),
                                    @ExampleObject(name = "endType과 audio 유무 불일치", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_AUDIO_PRESENCE",
                                              "message": "endType과 답변 음성 유무가 맞지 않아요."
                                            }
                                            """)
                            }
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "같은 질문에 이미 답변이 제출됨(재시도 차단)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "ANSWER_ALREADY_SUBMITTED",
                                      "message": "이미 제출된 답변이에요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewAnswerSubmitHttpResponse>> submitAnswer(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @Parameter(description = "답변 음성 파일(mp3). endType=SKIP이면 생략, HARD_CAP은 있어도 없어도 됨") MultipartFile audio,
            @Valid @ParameterObject InterviewAnswerSubmitHttpRequest request
    );
}
