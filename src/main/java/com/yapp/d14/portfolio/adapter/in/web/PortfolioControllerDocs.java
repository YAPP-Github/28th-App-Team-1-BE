package com.yapp.d14.portfolio.adapter.in.web;

import com.yapp.d14.portfolio.adapter.in.web.request.PortfolioRegisterHttpRequest;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioRegisterHttpResponse;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioStatusHttpResponse;
import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Portfolio", description = "포트폴리오 API")
public interface PortfolioControllerDocs {

    @Operation(
            summary = "포트폴리오 등록",
            description = "PDF 포트폴리오를 업로드합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 등록 즉시 `PROCESSING` 상태로 202를 반환하고, S3 업로드·파싱·임베딩은 비동기로 처리됩니다.\n" +
                    "- 계정당 포트폴리오는 1개만 등록할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "등록 접수 성공 — PROCESSING 상태로 생성"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "파일 형식·용량·페이지 수 위반",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "PDF 아님", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_FILE_TYPE",
                                              "message": "PDF 파일만 올릴 수 있어요"
                                            }
                                            """),
                                    @ExampleObject(name = "용량 초과", value = """
                                            {
                                              "success": false,
                                              "code": "FILE_TOO_LARGE",
                                              "message": "파일이 너무 커요. 20MB 이하 PDF로 올려주세요"
                                            }
                                            """),
                                    @ExampleObject(name = "페이지 초과", value = """
                                            {
                                              "success": false,
                                              "code": "PAGE_COUNT_EXCEEDED",
                                              "message": "페이지가 너무 많아요. 30페이지 이하 PDF로 올려주세요"
                                            }
                                            """),
                                    @ExampleObject(name = "손상된 PDF", value = """
                                            {
                                              "success": false,
                                              "code": "INVALID_PDF_FILE",
                                              "message": "파일이 손상된 것 같아요. 파일을 확인하고 다시 시도해 주세요"
                                            }
                                            """)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 포트폴리오가 존재함",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "PORTFOLIO_ALREADY_EXISTS",
                                      "message": "이미 등록된 포트폴리오가 있어요. 기존 포트폴리오를 삭제한 뒤 새로 올려주세요."
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<ApiResponse<PortfolioRegisterHttpResponse>> register(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "업로드할 PDF 파일") MultipartFile file,
            @Valid PortfolioRegisterHttpRequest request
    );

    @Operation(
            summary = "포트폴리오 처리 상태 조회",
            description = "포트폴리오 등록 후 처리 상태를 폴링으로 조회합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 3~5초 간격으로 폴링하며 `PROCESSING`(계속) / `READY`(완료) / `FAILED_FILE`·`FAILED_SYSTEM`(실패)을 응답합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
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
    ResponseEntity<ApiResponse<PortfolioStatusHttpResponse>> getStatus(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "포트폴리오 ID") UUID portfolioId
    );
}
