package com.yapp.d14.interview.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.interview.adapter.in.web.response.InterviewReportHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Tag(name = "Interview Report", description = "면접 보고서 조회·영상 API")
public interface InterviewReportControllerDocs {

    @Operation(
            summary = "면접 보고서 조회",
            description = "채점 파이프라인(#31)이 만들어 둔 결과를, 점수·판정·천장 같은 내부 원값 없이 " +
                    "사용자용 리포트 화면(한 줄 요약 + 항목 카드 + 영상 메타 + 지인 피드백 섹션) 형태로 반환합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 카드마다 대본(`transcript`)과 그 위에 칠할 하이라이트 구간(`highlightSpans`, 잘함/개선)이 내려옵니다. " +
                    "각 하이라이트에는 한 줄 제목(`title`)과 개선유형(`reason`)이 함께 내려오며, `reason`으로 카드 하단 안내를 결정합니다 — " +
                    "`PROBE_WORTHY`(꼬리질문 `followUpQuestions` 노출) / `OFF_INTENT`(질문 의도 리마인드) / `SHALLOW`·`SUFFICIENT`(코칭 한 줄만). " +
                    "`followUpQuestions`는 `reason=PROBE_WORTHY`일 때만 채워집니다.\n" +
                    "- 카드마다 질문/답변 대본을 문장 단위로 쪼갠 발화 구간(`scriptSegments`, 한 배열에 면접관·면접자 문장이 `role`로 구분되어 섞여 들어옵니다)이 내려옵니다. 각 문장의 `startSec`/`endSec`는 합성 영상(=녹화) 타임라인 기준이라, 영상 재생 위치와 맞춰 현재 발화 중인 문장을 강조할 수 있습니다.\n" +
                    "- 응답 최상위에는 카드와 별개로 `script` 배열(면접 전체 대본 타임라인)이 있습니다. 카드의 `scriptSegments`가 채점 대상 턴 안에서만의 문장이라면, `script`는 첫 면접관 멘트 → 프로젝트 설명 답변 → … → 마지막 마무리 멘트까지 세션의 모든 발화를 `startSec` 오름차순으로 담은 **한 배열**입니다. 영상 플레이어의 현재 발화 강조는 이 `script` 하나만 훑으면 됩니다.\n" +
                    "- 카드는 질문/답변 턴 하나당 하나입니다. 같은 항목(축)에 속한 카드끼리는 `axisOrder`가 같고, 그 안에서 `depthLevel`로 순서를 구분합니다 " +
                    "(화면 표시는 \"질문 {axisOrder}-{depthLevel}\", 예: 1-1, 1-2, 2-1 ...).\n" +
                    "- `status`는 채점 파이프라인의 진행 상태만 나타냅니다 — `GENERATING`(채점 중) / `READY`(생성 완료) / `INSUFFICIENT_ANALYSIS`(분석 부족) / `FAILED`(생성 실패).\n" +
                    "- `status=GENERATING`이면 `headline`/`redFlagNotices`/`video`/`cards`/`script`/`guestFeedback`이 모두 `null`입니다.\n" +
                    "- `status=INSUFFICIENT_ANALYSIS`이면 채점된 범위의 카드만 내려옵니다.\n" +
                    "- 심각한 레드플래그가 있는지는 `status`가 아니라 `redFlagNotices`가 비어 있는지로 판단합니다. `status=READY`이면서 `redFlagNotices`가 있으면 헤드라인이 중립 사실 요약으로 대체됩니다.\n" +
                    "- 카드 상단에 `resolutionNotice`가 있으면(해상도 낮음) 능력 판단성 분석을 보류한 상태이며, `highlightSpans`는 빈 배열입니다.\n" +
                    "- 레드플래그는 저장 5종 중 노출 3종(지어냄·모순·무결점 서사)만 중립 문구로 내려옵니다.\n" +
                    "- `video.url`은 영상이 만료되면 `null`이며, 그때도 카드의 대본·하이라이트는 그대로 유지됩니다.\n" +
                    "- `guestFeedback`은 지인이 한 명도 제출하지 않아도 `null`이 아니라 `participantCount=0`, `guests=[]`로 내려옵니다(프론트가 null 체크 없이 `guests`를 순회하고, \"아직 참여 없음\"을 `participantCount==0`으로 표현하게 하기 위함). 단 `status=GENERATING`일 때는 섹션 전체가 `null`입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "로딩 중", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "GENERATING",
                                                "headline": null,
                                                "redFlagNotices": null,
                                                "video": null,
                                                "cards": null,
                                                "script": null,
                                                "guestFeedback": null
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "정상", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "READY",
                                                "headline": "캐시 도입 결정의 이유와 한계까지 구체적인 수치로 설명해주셨어요.",
                                                "redFlagNotices": null,
                                                "video": {
                                                  "url": "https://cdn.example.com/videos/abc.mp4",
                                                  "expired": false,
                                                  "expiresAt": "2026-07-21T13:00:00"
                                                },
                                                "cards": [
                                                  {
                                                    "axisOrder": 1,
                                                    "depthLevel": 1,
                                                    "questionText": "Q. 결제 응답 속도를 개선하신 경험을 말씀해주세요. 무엇이 문제였나요?",
                                                    "transcript": "결제 화면에서 응답이 평균 800ms 정도로 느려서 사용자 이탈이 있었어요.",
                                                    "highlightSpans": [],
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": null,
                                                    "questionIntentTitle": "성능 저하 인지 수준",
                                                    "questionIntent": "성능 문제를 얼마나 구체적으로 인지했는지 확인하는 질문입니다.",
                                                    "scriptSegments": [
                                                      { "role": "INTERVIEWER", "text": "Q. 결제 응답 속도를 개선하신 경험을 말씀해주세요.", "startIndex": 0, "endIndex": 27, "startSec": 12.0, "endSec": 15.4 },
                                                      { "role": "INTERVIEWER", "text": " 무엇이 문제였나요?", "startIndex": 27, "endIndex": 37, "startSec": 15.4, "endSec": 16.8 },
                                                      { "role": "INTERVIEWEE", "text": "결제 화면에서 응답이 평균 800ms 정도로 느려서 사용자 이탈이 있었어요.", "startIndex": 0, "endIndex": 38, "startSec": 18.2, "endSec": 22.6 }
                                                    ]
                                                  },
                                                  {
                                                    "axisOrder": 1,
                                                    "depthLevel": 2,
                                                    "questionText": "Q. 응답이 느렸던 근본 원인은 무엇이었고, 어떻게 진단하셨나요?",
                                                    "transcript": "실제로 팀 프로젝트에서는 사용자 피드백을 50개 이상 모아 분석한 뒤...",
                                                    "highlightSpans": [
                                                      { "startIndex": 12, "endIndex": 48, "tone": "GOOD", "reason": "PROBE_WORTHY", "title": "구체적 수치로 원인 설명", "analysis": "구체적인 수치(50개, 분석 결과)를 근거로 원인을 설명해 신뢰도가 높습니다.", "followUpQuestions": ["그 수치는 어떤 기간을 기준으로 집계한 건가요?"], "startSec": 34.8 }
                                                    ],
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": null,
                                                    "questionIntentTitle": "근본 원인 진단 방법",
                                                    "questionIntent": "근본 원인을 어떤 체계적인 방법으로 찾아냈는지 확인하는 질문입니다."
                                                  },
                                                  {
                                                    "axisOrder": 2,
                                                    "depthLevel": 1,
                                                    "questionText": "Q. 트래픽이 10배일 때 가장 치명적인 지점과, 그 임계치를 어떻게 생각하시나요?",
                                                    "transcript": "트래픽이 10배로 늘면 결제 승인 API가 먼저 병목이 될 것 같고, DB 커넥션 풀이 임계치라고 봐요.",
                                                    "highlightSpans": [
                                                      { "startIndex": 12, "endIndex": 32, "tone": "GOOD", "reason": "SUFFICIENT", "title": "병목 지점 명확히 설명", "analysis": "병목 지점을 구체적으로 짚어 설명했습니다.", "followUpQuestions": [], "startSec": 61.2 }
                                                    ],
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": null,
                                                    "questionIntentTitle": "트래픽 확장 대응 전략",
                                                    "questionIntent": "트래픽이 증가했을 때 발생할 병목 지점과 시스템의 한계, 그리고 이를 어떻게 판단할지 설명하는 질문입니다."
                                                  }
                                                ],
                                                "script": [
                                                  { "role": "INTERVIEWER", "text": "안녕하세요, 오늘 면접을 진행하겠습니다.", "startSec": 0.0, "endSec": 3.2 },
                                                  { "role": "INTERVIEWER", "text": "Q. 결제 응답 속도를 개선하신 경험을 말씀해주세요.", "startSec": 12.0, "endSec": 15.4 },
                                                  { "role": "INTERVIEWEE", "text": "결제 화면에서 응답이 평균 800ms 정도로 느려서 사용자 이탈이 있었어요.", "startSec": 18.2, "endSec": 22.6 },
                                                  { "role": "INTERVIEWER", "text": "수고하셨습니다. 면접을 마치겠습니다.", "startSec": 70.5, "endSec": 73.9 }
                                                ],
                                                "guestFeedback": { "participantCount": 0, "guests": [] }
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "해상도 낮음 카드 포함", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "READY",
                                                "headline": "이번 면접에서는 결제 응답 속도 개선 경험을 중심으로 이야기를 나눴어요.",
                                                "redFlagNotices": null,
                                                "video": {
                                                  "url": "https://cdn.example.com/videos/abc.mp4",
                                                  "expired": false,
                                                  "expiresAt": "2026-07-21T13:00:00"
                                                },
                                                "cards": [
                                                  {
                                                    "axisOrder": 1,
                                                    "depthLevel": 1,
                                                    "questionText": "Q. 장애가 났을 때 어디부터 확인하시나요?",
                                                    "transcript": "저희 팀에서 진행한 프로젝트는 사용자 피드백을 반영해서...",
                                                    "highlightSpans": [],
                                                    "resolutionNotice": "질문의 의도와 다른 방향의 답변이었어요. 다음 연습 때는 질문이 묻는 것부터 짚고 시작해보세요.",
                                                    "cardRedFlagNotices": null,
                                                    "questionIntentTitle": "장애 원인 좁히기",
                                                    "questionIntent": "장애가 났을 때 원인을 어떻게 좁혀나가는지 확인하는 질문입니다."
                                                  }
                                                ],
                                                "script": [
                                                  { "role": "INTERVIEWER", "text": "Q. 장애가 났을 때 어디부터 확인하시나요?", "startSec": 10.0, "endSec": 13.5 },
                                                  { "role": "INTERVIEWEE", "text": "저희 팀에서 진행한 프로젝트는 사용자 피드백을 반영해서...", "startSec": 15.0, "endSec": 20.4 }
                                                ],
                                                "guestFeedback": { "participantCount": 0, "guests": [] }
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "분석 부족", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "INSUFFICIENT_ANALYSIS",
                                                "headline": "이번 면접의 답변이 충분하지 않아요. 다음 면접 연습 때는 조금 더 충분한 답변을 말씀해주세요.",
                                                "redFlagNotices": null,
                                                "video": {
                                                  "url": "https://cdn.example.com/videos/abc.mp4",
                                                  "expired": false,
                                                  "expiresAt": "2026-07-21T13:00:00"
                                                },
                                                "cards": [
                                                  {
                                                    "axisOrder": 1,
                                                    "depthLevel": 1,
                                                    "questionText": "Q. 최근에 성능을 개선한 경험이 있나요?",
                                                    "transcript": "네, 있습니다. 캐시를 좀 썼어요.",
                                                    "highlightSpans": [
                                                      { "startIndex": 8, "endIndex": 16, "tone": "IMPROVE", "reason": "SHALLOW", "title": "근거·수치 부족", "analysis": "무엇을 어떻게 개선했는지 구체적 근거나 수치가 없어 깊이가 부족합니다.", "followUpQuestions": [], "startSec": 15.0 }
                                                    ],
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": null,
                                                    "questionIntentTitle": "성능 개선 경험",
                                                    "questionIntent": "성능 문제를 어떻게 정의하고 개선했는지 확인하는 질문입니다."
                                                  }
                                                ],
                                                "script": [
                                                  { "role": "INTERVIEWER", "text": "Q. 최근에 성능을 개선한 경험이 있나요?", "startSec": 12.0, "endSec": 14.6 },
                                                  { "role": "INTERVIEWEE", "text": "네, 있습니다. 캐시를 좀 썼어요.", "startSec": 15.0, "endSec": 17.2 }
                                                ],
                                                "guestFeedback": { "participantCount": 0, "guests": [] }
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "레드플래그 포함", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "READY",
                                                "headline": "이번 면접에서는 캐시 도입 결정과 장애 대응 경험을 중심으로 이야기를 나눴어요.",
                                                "redFlagNotices": [
                                                  {
                                                    "type": "CONTRADICTION",
                                                    "message": "답변 사이에 사실관계가 엇갈린 지점이 있었어요. 실제 면접관은 이런 모순에 민감할 수 있습니다."
                                                  }
                                                ],
                                                "video": {
                                                  "url": "https://cdn.example.com/videos/abc.mp4",
                                                  "expired": false,
                                                  "expiresAt": "2026-07-21T13:00:00"
                                                },
                                                "cards": [
                                                  {
                                                    "axisOrder": 1,
                                                    "depthLevel": 1,
                                                    "questionText": "Q. 그 결정을 내리기까지 어떤 대안들을 검토하셨나요?",
                                                    "transcript": "제가 Redis 캐시를 도입했습니다...",
                                                    "highlightSpans": [],
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": [
                                                      {
                                                        "type": "CONTRADICTION",
                                                        "message": "답변 사이에 사실관계가 엇갈린 지점이 있었어요. 실제 면접관은 이런 모순에 민감할 수 있습니다."
                                                      }
                                                    ],
                                                    "questionIntentTitle": "의사결정 기여도",
                                                    "questionIntent": "의사결정 과정에서 본인의 역할과 기여를 확인하는 질문입니다."
                                                  }
                                                ],
                                                "script": [
                                                  { "role": "INTERVIEWER", "text": "Q. 그 결정을 내리기까지 어떤 대안들을 검토하셨나요?", "startSec": 40.0, "endSec": 43.8 },
                                                  { "role": "INTERVIEWEE", "text": "제가 Redis 캐시를 도입했습니다...", "startSec": 45.0, "endSec": 49.2 }
                                                ],
                                                "guestFeedback": { "participantCount": 0, "guests": [] }
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "지인 피드백 포함", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "READY",
                                                "headline": "캐시 도입 결정의 이유와 한계까지 구체적인 수치로 설명해주셨어요.",
                                                "redFlagNotices": null,
                                                "video": {
                                                  "url": "https://cdn.example.com/videos/abc.mp4",
                                                  "expired": false,
                                                  "expiresAt": "2026-08-04T13:00:00"
                                                },
                                                "cards": [],
                                                "script": [
                                                  { "role": "INTERVIEWER", "text": "안녕하세요, 오늘 면접을 진행하겠습니다.", "startSec": 0.0, "endSec": 3.2 },
                                                  { "role": "INTERVIEWEE", "text": "네, 잘 부탁드립니다.", "startSec": 4.0, "endSec": 5.4 }
                                                ],
                                                "guestFeedback": {
                                                  "participantCount": 2,
                                                  "guests": [
                                                    {
                                                      "alias": "허자연",
                                                      "attitudeRatings": [
                                                        { "axis": "GAZE", "level": 3, "comment": "꼬리질문에서 눈빛이 흔들려서 자신감이 없어 보였어요." },
                                                        { "axis": "EXPRESSION", "level": 4, "comment": null }
                                                      ]
                                                    },
                                                    {
                                                      "alias": "박민주",
                                                      "attitudeRatings": [
                                                        { "axis": "GAZE", "level": 2, "comment": null }
                                                      ]
                                                    }
                                                  ]
                                                }
                                              }
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "세션이 존재하지 않거나 본인 소유가 아님 (보고서가 아직 없으면 404가 아니라 status=GENERATING으로 응답)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "세션 없음", value = """
                                    {
                                      "success": false,
                                      "code": "INTERVIEW_SESSION_NOT_FOUND",
                                      "message": "면접 세션을 찾을 수 없어요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewReportHttpResponse>> getReport(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );
}
