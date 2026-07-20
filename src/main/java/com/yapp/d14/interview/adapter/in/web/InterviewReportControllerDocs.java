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

@Tag(name = "Interview Report", description = "면접 보고서 조회 API")
public interface InterviewReportControllerDocs {

    @Operation(
            summary = "면접 보고서 조회",
            description = "채점 파이프라인(#31)이 만들어 둔 결과를, 점수·판정·천장 같은 내부 원값 없이 " +
                    "사용자용 리포트 화면(한 줄 요약 + 항목 카드 + 영상 메타 + 지인 피드백 섹션) 형태로 반환합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 첫 화면이 곧 상세입니다. 별도 요약 화면·상세 시트 API 없이, 카드마다 `detail`(키워드·고쳐쓰기)이 함께 내려옵니다.\n" +
                    "- 카드는 질문/답변 턴 하나당 하나입니다. 같은 항목(축)에 속한 카드끼리는 `axisOrder`가 같고, 그 안에서 `depthLevel`로 순서를 구분합니다 " +
                    "(화면 표시는 \"질문 {axisOrder}-{depthLevel}\", 예: 1-1, 1-2, 2-1 ...).\n" +
                    "- `status`는 채점 파이프라인의 진행 상태만 나타냅니다 — `GENERATING`(채점 중) / `READY`(생성 완료) / `INSUFFICIENT_ANALYSIS`(분석 부족) / `FAILED`(생성 실패).\n" +
                    "- `status=GENERATING`이면 `headline`/`cards`/`video`/`guestFeedback`이 모두 `null`입니다. 3~5초 간격으로 폴링하세요.\n" +
                    "- `status=INSUFFICIENT_ANALYSIS`이면 채점된 범위의 카드만 내려옵니다.\n" +
                    "- 심각한 레드플래그가 있는지는 `status`가 아니라 `redFlagNotices`가 비어 있는지로 판단합니다. `status=READY`이면서 `redFlagNotices`가 있으면 헤드라인이 중립 사실 요약으로 대체됩니다.\n" +
                    "- 카드 상단에 `resolutionNotice`가 있으면(해상도 낮음) 능력 판단성 분석을 보류한 상태이며, `detail`은 `null`입니다.\n" +
                    "- 레드플래그는 저장 5종 중 노출 3종(지어냄·모순·무결점 서사)만 중립 문구로 내려옵니다.\n" +
                    "- `video.url`은 영상이 만료되면 `null`이며, 그때도 카드의 대본·하이라이트는 그대로 유지됩니다.\n" +
                    "- `guestFeedback`은 지인이 한 명도 제출하지 않았으면 `null`입니다."
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
                                                    "highlightSpans": null,
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": null,
                                                    "questionIntent": "성능 문제를 얼마나 구체적으로 인지했는지 확인하는 질문입니다.",
                                                    "detail": null
                                                  },
                                                  {
                                                    "axisOrder": 1,
                                                    "depthLevel": 2,
                                                    "questionText": "Q. 응답이 느렸던 근본 원인은 무엇이었고, 어떻게 진단하셨나요?",
                                                    "transcript": "실제로 팀 프로젝트에서는 사용자 피드백을 50개 이상 모아 분석한 뒤...",
                                                    "highlightSpans": [
                                                      { "startSec": 12.0, "endSec": 18.4, "tone": "GOOD" }
                                                    ],
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": null,
                                                    "questionIntent": "근본 원인을 어떤 체계적인 방법으로 찾아냈는지 확인하는 질문입니다.",
                                                    "detail": {
                                                      "actionKeywords": [
                                                        {
                                                          "keyword": "구체적인 사례 제시",
                                                          "problemAnalysis": "\\"캐시를 써서 빨라졌어요\\"까지만 답해서, 무엇이 문제였고 왜 그 방법이 통했는지가 드러나지 않았습니다.",
                                                          "improvementReason": "면접관은 무엇을 했는지보다 왜 그렇게 결정했는지를 평가합니다.",
                                                          "applicationMethod": "다음 면접에서는 문제 → 원인 → 해결 → 결과 순서로 사례를 설명해보세요.",
                                                          "priority": 1
                                                        }
                                                      ],
                                                      "rewrite": {
                                                        "originalQuote": "제가 만든 API가 좀 느렸는데, 캐시 붙여서 해결했어요.",
                                                        "rewrittenText": "API 응답이 느린 문제를 캐시를 도입해 해결했습니다."
                                                      }
                                                    }
                                                  },
                                                  {
                                                    "axisOrder": 2,
                                                    "depthLevel": 1,
                                                    "questionText": "Q. 트래픽이 10배일 때 가장 치명적인 지점과, 그 임계치를 어떻게 생각하시나요?",
                                                    "transcript": "실제로 팀 프로젝트에서는 사용자 피드백을 50개 이상 모아 분석한 뒤...",
                                                    "highlightSpans": [
                                                      { "startSec": 5.0, "endSec": 20.0, "tone": "GOOD" }
                                                    ],
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": null,
                                                    "questionIntent": "트래픽이 증가했을 때 발생할 병목 지점과 시스템의 한계, 그리고 이를 어떻게 판단할지 설명하는 질문입니다.",
                                                    "detail": {
                                                      "actionKeywords": [],
                                                      "rewrite": null
                                                    }
                                                  }
                                                ],
                                                "guestFeedback": null
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
                                                    "highlightSpans": null,
                                                    "resolutionNotice": "질문의 의도와 다른 방향의 답변이었어요. 다음 연습 때는 질문이 묻는 것부터 짚고 시작해보세요.",
                                                    "cardRedFlagNotices": null,
                                                    "questionIntent": "장애가 났을 때 원인을 어떻게 좁혀나가는지 확인하는 질문입니다.",
                                                    "detail": null
                                                  }
                                                ],
                                                "guestFeedback": null
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
                                                "cards": [],
                                                "guestFeedback": null
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
                                                    "highlightSpans": null,
                                                    "resolutionNotice": null,
                                                    "cardRedFlagNotices": [
                                                      {
                                                        "type": "CONTRADICTION",
                                                        "message": "답변 사이에 사실관계가 엇갈린 지점이 있었어요. 실제 면접관은 이런 모순에 민감할 수 있습니다."
                                                      }
                                                    ],
                                                    "questionIntent": "의사결정 과정에서 본인의 역할과 기여를 확인하는 질문입니다.",
                                                    "detail": null
                                                  }
                                                ],
                                                "guestFeedback": null
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
                    description = "세션이 존재하지 않거나 본인 소유가 아니거나, 보고서가 아직 생성되지 않음",
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
                                    @ExampleObject(name = "보고서 없음", value = """
                                            {
                                              "success": false,
                                              "code": "INTERVIEW_REPORT_NOT_FOUND",
                                              "message": "면접 보고서를 찾을 수 없어요."
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<InterviewReportHttpResponse>> getReport(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "면접 세션 ID") @PathVariable Long sessionId
    );
}
