package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.request.InterviewAnswerSubmitHttpRequest;
import com.yapp.d14.interview.adapter.in.web.request.InterviewSessionCreateHttpRequest;
import com.yapp.d14.interview.adapter.in.web.response.InterviewAnswerSubmitHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewReportListHttpResponse;
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
            summary = "내 면접 레포트 목록 조회",
            description = "마이페이지 '내 면접 레포트' 목록을 최신순으로 조회합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 레포트 생성이 시도된(Report가 존재하는) 세션만 포함합니다. 준비중·진행중 세션은 제외됩니다.\n" +
                    "- 직군·연차·포트폴리오 파일명·JD는 면접 진행 당시의 스냅샷 값입니다.\n" +
                    "- `portfolioDeleted=true`면 사용한 포트폴리오가 이후 삭제된 것으로, '삭제된 포트폴리오' 배지를 표시합니다.\n" +
                    "- `jdUrl`이 null이면 JD를 직접 입력했거나 없이 진행한 것으로 '직접 입력함' 등으로 표기합니다.\n" +
                    "- `reportStatus=FAILED`면 레포트 생성에 실패한 것이며, 이 경우 이용권은 차감되지 않습니다.\n" +
                    "- `feedbackAvailable=true`면 지인 피드백 링크를 새로 요청할 수 있습니다(레포트 READY & 공유 링크 미생성)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = InterviewReportListHttpResponse.class))
            )
    })
    ResponseEntity<ApiResponse<InterviewReportListHttpResponse>> getReportList(@Parameter(hidden = true) @CurrentUser UUID userId);

    @Operation(
            summary = "면접 세션 생성",
            description = "포트폴리오(및 선택적으로 JD·집중 프로젝트 설명)를 받아 면접 세션을 생성합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 이용권 확인 → 입력 검증 → 항목별 가중치·질문 예산 계산까지 동기로 처리한 뒤, `PROCESSING` 상태로 202를 즉시 반환합니다.\n" +
                    "- 질문 후보 풀 생성(Preload) 등 이후 단계는 비동기로 처리되며, `statusUrl`로 상태를 폴링합니다.\n" +
                    "- `portfolioId`로 지정한 포트폴리오는 반드시 `READY` 상태여야 합니다.\n" +
                    "- 직군·연차는 요청으로 받지 않고, `PATCH /api/v1/users/me/profile`로 등록한 회원 프로필 값을 생성 시점 스냅샷으로 사용합니다. " +
                    "직군 또는 연차가 아직 등록되어 있지 않으면 `USER_PROFILE_NOT_REGISTERED`로 거부됩니다.\n" +
                    "- `jdUrl`과 `jdText`는 상호 배타적입니다(동시 입력 시 `JD_URL_AND_TEXT_BOTH_PROVIDED`). `jdUrl`은 `/api/v1/jd/validate`로 먼저 검증(캐싱, 6시간 TTL)돼 있어야 하며, " +
                    "검증 후 캐시가 만료된 채로 세션을 생성하면 `JD_CONTENT_NOT_FOUND`로 거부됩니다(재검증 필요).\n" +
                    "- `freeText`(집중 프로젝트 설명)를 입력하면 포트폴리오와의 연관성을 임베딩 유사도로 검사합니다.\n" +
                    "- 계정당 이용권(무료 3회)이 소진되면 세션을 생성할 수 없습니다.\n" +
                    "- 정지된 계정은 `ACCOUNT_SUSPENDED`(403)로 차단됩니다. 면접 시작만 막히고 로그인·레포트 열람·마이페이지·탈퇴는 그대로 사용할 수 있습니다.\n" +
                    "- 필수 약관 동의가 최신이 아니면 `CONSENT_VERSION_STALE`(403)로 차단됩니다. 재동의 화면으로 보낸 뒤 다시 시도하세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "생성 접수 성공 — JD·freeText 조합(JD URL/JD 텍스트/JD 없음, freeText 유무)과 무관하게 항상 이 모양으로 PROCESSING 상태 반환",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InterviewSessionCreateHttpResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sessionId": 101,
                                        "status": "PROCESSING",
                                        "statusUrl": "/api/v1/interview/sessions/101/status"
                                      }
                                    }
                                    """)
                    )
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
                                    @ExampleObject(name = "직무·연차 미등록", value = """
                                            {
                                              "success": false,
                                              "code": "USER_PROFILE_NOT_REGISTERED",
                                              "message": "면접을 시작하려면 먼저 직무와 연차를 등록해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "JD URL과 텍스트 동시 입력", value = """
                                            {
                                              "success": false,
                                              "code": "JD_URL_AND_TEXT_BOTH_PROVIDED",
                                              "message": "jdUrl과 jdText는 함께 입력할 수 없어요."
                                            }
                                            """),
                                    @ExampleObject(name = "JD 미검증", value = """
                                            {
                                              "success": false,
                                              "code": "JD_NOT_VALIDATED",
                                              "message": "JD 링크를 먼저 검증해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "JD 캐시 만료(검증 이후 시간 경과)", value = """
                                            {
                                              "success": false,
                                              "code": "JD_CONTENT_NOT_FOUND",
                                              "message": "JD 링크의 캐시가 만료됐어요. 다시 검증해 주세요."
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
                    description = "면접 시작 게이트 차단 — 게이트1(계정 정지) → 게이트3(재동의) → 이용권 순으로 검사하며, 여러 개가 겹치면 앞선 게이트의 코드가 반환됩니다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "게이트1 — 계정 정지", value = """
                                            {
                                              "success": false,
                                              "code": "ACCOUNT_SUSPENDED",
                                              "message": "비정상적인 이용 패턴이 반복 확인되어 면접 시작이 제한되었어요."
                                            }
                                            """),
                                    @ExampleObject(name = "게이트3 — 재동의 필요", value = """
                                            {
                                              "success": false,
                                              "code": "CONSENT_VERSION_STALE",
                                              "message": "약관이 바뀌어 다시 동의가 필요해요."
                                            }
                                            """),
                                    @ExampleObject(name = "남은 이용권 없음", value = """
                                            {
                                              "success": false,
                                              "code": "NO_REMAINING_TICKET",
                                              "message": "남은 이용권이 없어요."
                                            }
                                            """)
                            }
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
                    "  - `endType=BACK_EXIT`: 사용자가 뒤로가기 버튼을 눌러 이탈 — audio가 있으면 STT만 기록하고 즉시 종료(wrapUpMessage 없음).\n" +
                    "  - `endType=MANUAL_END`: 사용자가 면접 종료 버튼을 눌러 수동 종료 — 즉시 종료하며 짧은 마무리 멘트를 반환합니다.\n" +
                    "  - `endType=HARD_CAP`: 12:00 경과 강제 종료 — audio 유무와 무관하게 즉시 종료합니다.\n" +
                    "  - 직전에 받은 질문이 마무리(wrap-up) 질문이었던 경우, endType 없이도 자연 종료됩니다.\n" +
                    "  - 위 종료 경로에서는 `nextQuestion`이 `null`, `sessionEnded`가 `true`이며, 이용권은 보류(HELD) 상태를 유지한 채 리포트 생성이 비동기로 트리거됩니다 — 확정(commit)은 리포트 생성 성공 시점에 이뤄집니다.\n" +
                    "  - 세션 전체 누적 STT 인식 실패율이 30%를 초과하면 `endType`과 무관하게 즉시 세션이 무효화되어 종료됩니다(`sessionEnded=true`, `endType=STT_RESET`, `wrapUpMessage=null`, 리포트 생성 없음). 이때는 이용권이 차감되지 않고 환불(release)됩니다.\n" +
                    "  - `endType` 응답 필드로 종료 사유를 구분할 수 있습니다: `NORMAL_END`/`MANUAL_END`/`HARD_CAP`/`BACK_EXIT`/`STT_RESET`. 세션이 끝나지 않았으면 `null`입니다.\n" +
                    "  - 그 외에는 매 턴 루프로 이어집니다(현재 구현 중), `sessionEnded`는 `false`입니다.\n" +
                    "- `wrapUpMessage.ttsAudio`는 마무리 멘트 음성을 base64로 인코딩한 mp3입니다(BACK_EXIT은 `wrapUpMessage` 자체가 `null`). " +
                    "고정 문구 3종(MANUAL_END/HARD_CAP/자연종료)은 최초 요청 시 TTS로 합성해 S3에 캐시하고 이후에는 캐시를 재사용합니다.\n" +
                    "- `isWrapUp`은 클라이언트 타이머 기준 8:45 경과 여부이며, 다음 질문을 마무리 질문으로 만들지 여부에 사용됩니다.\n" +
                    "- `audio` 파트는 선택적입니다. `endType=SKIP`이면 audio가 없어야 하고, `endType=null`이면 audio가 있어야 합니다. " +
                    "`MANUAL_END`/`HARD_CAP`/`BACK_EXIT`은 audio 유무와 무관합니다.\n" +
                    "- 응답에는 오디오가 동봉되지 않습니다. `nextQuestion.questionId`로 " +
                    "`GET /{sessionId}/questions/{questionId}/audio/stream`을 호출해 오디오를 받으세요.\n" +
                    "- STT·답변 분석·질문 생성·TTS 등 내부 AI 호출은 1회 자동 재시도되며, 그래도 실패하면 503(`AI_TEMPORARILY_UNAVAILABLE`)을 반환합니다. " +
                    "이 시점엔 아직 아무것도 저장되지 않으므로, 같은 sessionId·questionId·audio로 이 API를 그대로 다시 호출하면 됩니다."
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
                                                "endType": null
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "세션 종료 — NORMAL_END(자연 종료)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "answerId": 12,
                                                "nextQuestion": null,
                                                "sessionEnded": true,
                                                "wrapUpMessage": {
                                                  "ttsAudio": "base64로 인코딩된 mp3"
                                                },
                                                "endType": "NORMAL_END"
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "세션 종료 — MANUAL_END(수동 종료)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "answerId": 12,
                                                "nextQuestion": null,
                                                "sessionEnded": true,
                                                "wrapUpMessage": {
                                                  "ttsAudio": "base64로 인코딩된 mp3"
                                                },
                                                "endType": "MANUAL_END"
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "세션 종료 — HARD_CAP(최대 한도 도달)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "answerId": 12,
                                                "nextQuestion": null,
                                                "sessionEnded": true,
                                                "wrapUpMessage": {
                                                  "ttsAudio": "base64로 인코딩된 mp3"
                                                },
                                                "endType": "HARD_CAP"
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "세션 종료 — BACK_EXIT(뒤로가기, 마무리 멘트 없음)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "answerId": 12,
                                                "nextQuestion": null,
                                                "sessionEnded": true,
                                                "wrapUpMessage": null,
                                                "endType": "BACK_EXIT"
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "세션 종료 — STT_RESET(STT 인식 실패로 무효화)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "answerId": 12,
                                                "nextQuestion": null,
                                                "sessionEnded": true,
                                                "wrapUpMessage": null,
                                                "endType": "STT_RESET"
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
                                            """),
                                    @ExampleObject(name = "지원하지 않는 답변 음성 형식", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_AUDIO_FORMAT",
                                              "message": "답변 음성은 m4a 형식만 업로드할 수 있어요."
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
                    description = "재시도 차단 — 같은 질문에 이미 답변이 제출됐거나 세션이 이미 종료됨",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "이미 제출된 답변", value = """
                                            {
                                              "success": false,
                                              "code": "ANSWER_ALREADY_SUBMITTED",
                                              "message": "이미 제출된 답변이에요."
                                            }
                                            """),
                                    @ExampleObject(name = "이미 종료된 세션", value = """
                                            {
                                              "success": false,
                                              "code": "SESSION_ALREADY_ENDED",
                                              "message": "이미 종료된 면접 세션이에요."
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "AI 연동 서버 일시 장애 — STT·답변 분석·질문 생성·TTS 중 하나가 재시도(1회)까지 실패함. " +
                            "이 시점엔 아무것도 저장되지 않으므로 같은 요청으로 이 API를 그대로 다시 호출하면 됩니다.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "AI_TEMPORARILY_UNAVAILABLE",
                                      "message": "일시적인 오류예요. 같은 답변을 다시 제출해 주세요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewAnswerSubmitHttpResponse>> submitAnswer(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @Parameter(description = "답변 음성 파일(m4a). endType=SKIP이면 생략, HARD_CAP은 있어도 없어도 됨") MultipartFile audio,
            @Valid @ParameterObject InterviewAnswerSubmitHttpRequest request
    );
}
