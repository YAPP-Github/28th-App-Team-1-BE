package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.request.InterviewAbandonHttpRequest;
import com.yapp.d14.interview.adapter.in.web.response.InterviewAbandonHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionResumeConfirmHttpResponse;
import com.yapp.d14.interview.adapter.in.web.response.InterviewSessionResumeHttpResponse;
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

@Tag(name = "Interview Resume", description = "면접 세션 중단 후 재접속 처리 API")
public interface InterviewResumeControllerDocs {

    @Operation(
            summary = "면접 재개 가능 여부 조회",
            description = "특정 세션에 대해 재접속 화면(이어서 진행하기 / 중단하기)을 띄울지 판단하는 순수 조회 API입니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 앱 실행·포그라운드 진입 시 클라이언트가 메모리에 들고 있던 sessionId로 가볍게 호출할 수 있습니다.\n" +
                    "- `resumeState=RESUMABLE`: 세션이 `IN_PROGRESS`이고 이용권 예약이 아직 `HELD`이며 `heldAt`이 20분 이내인 경우입니다. " +
                    "이때만 `startedAt`(면접 시작 시각)·`elapsedSeconds`(벽시계 기준 경과초)가 채워지고 `status`는 `null`입니다.\n" +
                    "- `resumeState=ENDED`: 이미 종료된 세션이거나, 또는 예약이 더 이상 `HELD`가 아니거나 `heldAt`이 20분을 초과한 경우입니다. " +
                    "이때는 `status`(`COMPLETED`/`ABANDONED`/`INVALID`)만 채워지고 `startedAt`/`elapsedSeconds`는 `null`입니다.\n" +
                    "- hold 만료가 원인인 `ENDED`(예약 만료·미보유)라면, 이 호출 안에서 서버가 그 자리에서 세션을 `ABANDONED`(중단사유 `HOLD_EXPIRED`)로 전환하고 " +
                    "이용권도 즉시 환불 처리합니다 — 클라이언트가 별도로 처리할 것은 없습니다.\n" +
                    "- 이 API는 어떤 질문을 이어서 답할지는 알려주지 않습니다. 사용자가 실제로 \"이어서 진행하기\"를 누르면 " +
                    "`POST /{sessionId}/resume`을 호출하세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "재개 가능", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "resumeState": "RESUMABLE",
                                                "startedAt": "2026-07-27T10:00:00",
                                                "elapsedSeconds": 305,
                                                "status": null
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "이미 종료됨 — 정상 완료", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "resumeState": "ENDED",
                                                "startedAt": null,
                                                "elapsedSeconds": null,
                                                "status": "COMPLETED"
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "이미 종료됨 — 중단됨(중단하기 또는 hold 만료)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "resumeState": "ENDED",
                                                "startedAt": null,
                                                "elapsedSeconds": null,
                                                "status": "ABANDONED"
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "이미 종료됨 — STT 인식 실패로 무효화", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "resumeState": "ENDED",
                                                "startedAt": null,
                                                "elapsedSeconds": null,
                                                "status": "INVALID"
                                              }
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 없거나 본인 소유가 아님 — 존재 여부를 노출하지 않기 위해 두 경우 모두 같은 응답으로 수렴",
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
    ResponseEntity<ApiResponse<InterviewSessionResumeHttpResponse>> getResume(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );

    @Operation(
            summary = "면접 재개 확정",
            description = "재접속 화면에서 \"이어서 진행하기\"를 눌렀을 때만 호출합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 정상 케이스 — 응답은 기존 `POST /{sessionId}/answers` 응답과 같은 모양입니다(`answerId`만 없음). " +
                    "`nextQuestion`은 세션의 최신 턴 질문이며, 클라이언트는 이전 상태를 신경 쓸 필요 없이 이 질문을 재생하고 답을 받으면 됩니다. " +
                    "질문 오디오는 `GET /{sessionId}/questions/{questionId}/audio/stream`으로 별도 요청하세요.\n" +
                    "- 레이스 케이스 — `GET /{sessionId}/resume` 확인과 이 호출 사이에 다른 경로(다른 기기의 세션 생성 등)가 끼어들어 " +
                    "이용권 예약이 더 이상 유효하지 않게 된 경우, `409`가 아니라 `200`으로 `sessionEnded=true`를 반환합니다. " +
                    "이때 세션은 `ABANDONED`(중단사유 `HOLD_EXPIRED`)로 전환되고 이용권도 즉시 환불 처리됩니다.\n" +
                    "- `409 SESSION_ALREADY_ENDED`는 `GET /{sessionId}/resume`을 거치지 않고 이미 종료된 세션에 바로 호출한 비정상적인 경우에만 발생합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재개 확정 성공 — 다음 질문 또는 hold 만료 레이스로 인한 세션 종료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "다음 질문 반환", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "nextQuestion": {
                                                  "questionId": 456,
                                                  "isLast": false,
                                                  "turn": { "turnLevel": 2, "depthLevel": 1 }
                                                },
                                                "sessionEnded": false,
                                                "wrapUpMessage": null,
                                                "endType": null,
                                                "status": null,
                                                "abandonCause": null,
                                                "endedAt": null
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "hold 만료 레이스 — 세션 종료", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "nextQuestion": null,
                                                "sessionEnded": true,
                                                "wrapUpMessage": null,
                                                "endType": null,
                                                "status": "ABANDONED",
                                                "abandonCause": "HOLD_EXPIRED",
                                                "endedAt": "2026-07-27T10:31:02"
                                              }
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 없거나 본인 소유가 아님 — 존재 여부를 노출하지 않기 위해 두 경우 모두 같은 응답으로 수렴",
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "GET /{sessionId}/resume을 거치지 않고 이미 종료된 세션에 호출한 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "SESSION_ALREADY_ENDED",
                                      "message": "이미 종료된 면접 세션이에요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewSessionResumeConfirmHttpResponse>> confirmResume(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );

    @Operation(
            summary = "면접 중단",
            description = "재접속 화면에서 \"중단하기\"를 눌렀을 때 호출합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- `cause`가 이용권 분기의 유일한 입력값입니다. 경과 시간이나 진행량은 판정에 쓰이지 않습니다.\n" +
                    "  - `cause=NETWORK_DISCONNECT`: 네트워크 끊김으로 인한 중단 — 이용권 환급(`ticketOutcome=RELEASED`), 리포트 미생성.\n" +
                    "  - `cause=USER_EXIT`: 사용자의 의도적 이탈로 인한 중단 — 이용권 차감(`ticketOutcome=COMMITTED`), 리포트 생성 트리거(`reportGenerating=true`). " +
                    "답변이 적어 해상도가 낮아도 리포트는 생성됩니다(`INSUFFICIENT_ANALYSIS`로 생성될 수 있음).\n" +
                    "- `cause`로 `HOLD_EXPIRED`는 보낼 수 없습니다 — 서버가 hold 만료를 스스로 감지했을 때만 쓰는 내부 전용 값입니다. " +
                    "누락·정의되지 않은 값·`HOLD_EXPIRED` 세 경우 모두 `400 INVALID_ABANDON_CAUSE`로 동일하게 거부됩니다.\n" +
                    "- 중단하기 버튼을 중복으로 누르면 두 번째 요청은 `409 SESSION_ALREADY_ENDED`가 됩니다. " +
                    "클라이언트는 이 409를 에러가 아니라 \"이미 중단 완료\"로 간주하고 종료 화면으로 진행하면 됩니다(별도 멱등키 불필요)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "중단 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "네트워크 끊김 — 이용권 환급", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "sessionId": 123,
                                                "status": "ABANDONED",
                                                "abandonCause": "NETWORK_DISCONNECT",
                                                "endedAt": "2026-07-27T10:06:41",
                                                "ticketOutcome": "RELEASED",
                                                "reportGenerating": false
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "사용자 이탈 — 이용권 차감 + 리포트 생성", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "sessionId": 123,
                                                "status": "ABANDONED",
                                                "abandonCause": "USER_EXIT",
                                                "endedAt": "2026-07-27T10:06:41",
                                                "ticketOutcome": "COMMITTED",
                                                "reportGenerating": true
                                              }
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "cause 누락, 정의되지 않은 값, 또는 HOLD_EXPIRED(서버 전용 값) — 세 경우 모두 같은 응답",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "INVALID_ABANDON_CAUSE",
                                      "message": "지원하지 않는 중단 사유예요."
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 없거나 본인 소유가 아님 — 존재 여부를 노출하지 않기 위해 두 경우 모두 같은 응답으로 수렴",
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 COMPLETED/ABANDONED/INVALID인 세션 — 중복 탭이면 이미 중단 완료로 간주하고 넘어가면 됨",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "SESSION_ALREADY_ENDED",
                                      "message": "이미 종료된 면접 세션이에요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewAbandonHttpResponse>> abandon(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody InterviewAbandonHttpRequest request
    );
}
