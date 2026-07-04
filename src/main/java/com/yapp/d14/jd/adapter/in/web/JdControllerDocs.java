package com.yapp.d14.jd.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.jd.adapter.in.web.request.JdValidateHttpRequest;
import com.yapp.d14.jd.adapter.in.web.response.JdValidateHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "JD", description = "JD 크롤링/검증 API")
public interface JdControllerDocs {

    @Operation(
            summary = "JD URL 유효성 검증",
            description = "JD URL을 크롤링해 텍스트를 추출하고 Redis에 캐싱합니다.\n\n" +
                    "크롤링에 실패했거나 추출된 본문이 200자 미만이면 `data.valid: false`로 응답합니다. " +
                    "이 경우 클라이언트는 JD 본문 직접 입력으로 폴백해야 합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "요청 처리 완료 (크롤링 성공 여부는 data.valid로 확인)",
                    content = @Content(
                            schema = @Schema(implementation = JdValidateHttpResponse.class),
                            examples = {
                                    @ExampleObject(name = "크롤링 성공", value = """
                                            {
                                              "success": true,
                                              "data": { "valid": true, "reason": null, "message": null }
                                            }
                                            """),
                                    @ExampleObject(name = "크롤링 실패", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "valid": false,
                                                "reason": "CRAWLING_FAILED",
                                                "message": "공고 페이지에 접속할 수 없어요. 공고 내용을 직접 붙여넣어 주세요."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "본문 부족", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "valid": false,
                                                "reason": "CONTENT_TOO_SHORT",
                                                "message": "공고 내용을 충분히 가져오지 못했어요. 공고 내용을 직접 붙여넣어 주세요."
                                              }
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류 (jdUrl 누락 또는 URL 형식 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "INVALID_JD_URL",
                                      "message": "올바른 URL 형식이 아니에요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<JdValidateHttpResponse>> validate(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody JdValidateHttpRequest request
    );
}
