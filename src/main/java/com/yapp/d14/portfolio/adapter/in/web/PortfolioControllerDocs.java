package com.yapp.d14.portfolio.adapter.in.web;

import com.yapp.d14.portfolio.adapter.in.web.request.PortfolioRegisterHttpRequest;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioDeleteHttpResponse;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioListHttpResponse;
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
import org.springdoc.core.annotations.ParameterObject;
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
                    "- 계정당 포트폴리오는 1개만 등록할 수 있습니다(기존 포트폴리오가 있으면 먼저 삭제해야 함).\n" +
                    "- 삭제 후 재업로드(교체)는 캘린더 월 1회로 제한됩니다(매월 1일 0시 서버 시간 리셋). 최초 업로드는 이 제한과 무관합니다."
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
                    description = "이미 포트폴리오가 존재하거나, 이번 달 재업로드 횟수를 이미 사용함",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "활성 포트폴리오 존재", value = """
                                            {
                                              "success": false,
                                              "code": "PORTFOLIO_ALREADY_EXISTS",
                                              "message": "이미 등록된 포트폴리오가 있어요. 기존 포트폴리오를 삭제한 뒤 새로 올려주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "이번 달 재업로드 횟수 소진", value = """
                                            {
                                              "success": false,
                                              "code": "REPLACEMENT_LIMIT_EXCEEDED",
                                              "message": "포트폴리오 재업로드는 한 달에 한 번만 가능해요. 다음 달 1일부터 다시 시도해 주세요."
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<PortfolioRegisterHttpResponse>> register(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "업로드할 PDF 파일") MultipartFile file,
            @Valid @ParameterObject PortfolioRegisterHttpRequest request
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

    @Operation(
            summary = "내 포트폴리오 목록 조회",
            description = "로그인한 사용자가 등록한 포트폴리오 목록을 조회합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- MVP는 계정당 1개로 제한되지만, 응답은 향후 다건 확장을 고려해 배열로 내려갑니다.\n" +
                    "- 소프트 삭제된 포트폴리오는 응답에서 제외됩니다.\n" +
                    "- `replaceAvailable`: 이번 달 재업로드(삭제 후 교체) 가능 여부. `nextAvailableAt`: 재업로드가 막혀 있을 때만 값이 채워지며 다시 가능해지는 시각(다음 달 1일 0시)을 나타냅니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            )
    })
    ResponseEntity<ApiResponse<PortfolioListHttpResponse>> getList(
            @Parameter(hidden = true) @CurrentUser UUID userId
    );

    @Operation(
            summary = "포트폴리오 삭제",
            description = "포트폴리오를 삭제합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공"
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
    ResponseEntity<ApiResponse<PortfolioDeleteHttpResponse>> delete(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "포트폴리오 ID") UUID portfolioId
    );
}
