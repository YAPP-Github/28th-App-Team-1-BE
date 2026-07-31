package com.yapp.d14.consent.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.consent.adapter.in.web.request.ConsentSubmitHttpRequest;
import com.yapp.d14.consent.adapter.in.web.response.ConsentDocumentHttpResponse;
import com.yapp.d14.consent.adapter.in.web.response.ConsentPendingItemsHttpResponse;
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

@Tag(name = "Consent", description = "약관 동의 API")
public interface ConsentControllerDocs {

    @Operation(
            summary = "동의 제출",
            description = "동의 항목을 제출합니다. 최초 온보딩(필수 5종 전체)과 재동의(GET /pending 이 내려준 항목만)에 " +
                    "모두 쓰이는 공용 API입니다.\n\n" +
                    "- `items[].version`은 GET /api/v1/consents/pending 이 내려준 버전을 그대로 보내야 하며, " +
                    "서버의 현행 버전과 다르면 `CONSENT_VERSION_MISMATCH`로 거부됩니다(목록을 다시 조회한 뒤 재시도).\n" +
                    "- 필수 항목은 `agreed:false`를 보낼 수 없습니다 — 포함됐는데 거부거나, 최초 제출인데 " +
                    "필수 항목이 하나라도 빠지면 `REQUIRED_CONSENT_MISSING`으로 거부됩니다.\n" +
                    "- 선택 항목은 `agreed:false`(거부)도 정상 제출이며, 거부 이력도 그대로 저장됩니다.\n" +
                    "- 이 유저의 첫 성공한 제출 시 무료 면접 이용권 3회가 부여됩니다(이후 재동의 제출에서는 부여되지 않음).\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "제출 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "제출 성공", value = """
                                            {
                                              "success": true
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류, 동의 항목 누락/거부, 지원하지 않는 항목 코드, 또는 버전 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "요청 값 오류(항목 누락 등)", value = """
                                            {
                                              "success": false,
                                              "code": "VALIDATION_ERROR",
                                              "message": "동의 항목을 1개 이상 입력해주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "지원하지 않는 항목 코드", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_CONSENT_ITEM",
                                              "message": "지원하지 않는 동의 항목이에요."
                                            }
                                            """),
                                    @ExampleObject(name = "필수 항목 누락 또는 거부", value = """
                                            {
                                              "success": false,
                                              "code": "REQUIRED_CONSENT_MISSING",
                                              "message": "필수 동의 항목이 누락됐어요."
                                            }
                                            """),
                                    @ExampleObject(name = "버전 불일치(목록을 다시 조회해야 함)", value = """
                                            {
                                              "success": false,
                                              "code": "CONSENT_VERSION_MISMATCH",
                                              "message": "동의 항목 버전이 최신이 아니에요. 목록을 다시 조회해주세요."
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<Void>> submit(
            @Parameter(hidden = true) UUID userId,
            @Valid ConsentSubmitHttpRequest request
    );

    @Operation(
            summary = "재동의/최초 동의 대상 조회",
            description = "지금 동의가 필요한 항목 목록을 조회합니다.\n\n" +
                    "- 필수 항목 중 아직 한 번도 동의한 적 없는 항목\n" +
                    "- 과거에 동의했지만 현행 버전보다 낮은 버전으로 동의된 항목(선택 항목 포함)\n\n" +
                    "신규 유저는 필수 5종 전체가, 구버전 동의 유저는 바뀐 항목만 반환됩니다. " +
                    "응답의 `status`로 최초 동의(NOT_SUBMITTED)인지 재동의(STALE)인지 이 API 하나만으로 구분할 수 있고, " +
                    "각 항목에는 필수 여부(`required`)와 제목(`label`)이 함께 내려갑니다. " +
                    "각 항목의 본문은 GET /api/v1/consents/{item}/versions/{version} 으로 별도 조회합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공. `status`와 `items`는 회원의 동의 상태에 따라 달라짐",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConsentPendingItemsHttpResponse.class),
                            examples = {
                                    @ExampleObject(name = "최초 동의 필요(신규 온보딩)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "NOT_SUBMITTED",
                                                "items": [
                                                  { "code": "AGE_OVER_14", "label": "만 14세 이상", "required": true, "version": 1, "hasDocument": true },
                                                  { "code": "TERMS_OF_SERVICE", "label": "서비스 이용약관", "required": true, "version": 1, "hasDocument": true },
                                                  { "code": "PERSONAL_INFO_COLLECTION", "label": "개인정보 수집·이용", "required": true, "version": 1, "hasDocument": true },
                                                  { "code": "INTERVIEW_RECORDING", "label": "면접 영상·음성 촬영·저장", "required": true, "version": 1, "hasDocument": true },
                                                  { "code": "OVERSEAS_TRANSFER", "label": "개인정보 국외 이전", "required": true, "version": 1, "hasDocument": false }
                                                ]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "재동의 필요(일부 항목 버전 변경)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "STALE",
                                                "items": [
                                                  { "code": "TERMS_OF_SERVICE", "label": "서비스 이용약관", "required": true, "version": 2, "hasDocument": true }
                                                ]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "동의 최신 상태(재동의 불필요)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "status": "UP_TO_DATE",
                                                "items": []
                                              }
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<ConsentPendingItemsHttpResponse>> getPendingItems(@Parameter(hidden = true) UUID userId);

    @Operation(
            summary = "동의 문서 본문 조회",
            description = "특정 항목의 특정 버전 본문(제목·내용)을 조회합니다. 클라이언트가 항목을 탭했을 때 " +
                    "바텀시트에 전문을 띄우는 용도입니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConsentDocumentHttpResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "item": "TERMS_OF_SERVICE",
                                        "version": 1,
                                        "title": "서비스 이용약관",
                                        "content": "제1조 (목적)\\n이 약관은...\\n\\n제2조 (정의)\\n..."
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 항목 코드",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "INVALID_CONSENT_ITEM",
                                      "message": "지원하지 않는 동의 항목이에요."
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 문서(해당 항목·버전 조합의 본문이 등록되지 않음)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "DOCUMENT_NOT_FOUND",
                                      "message": "존재하지 않는 동의 문서예요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<ConsentDocumentHttpResponse>> getDocument(
            @Parameter(description = "동의 항목 코드", example = "TERMS_OF_SERVICE") String item,
            @Parameter(description = "문서 버전", example = "2") int version
    );
}
