package com.yapp.d14.portfolio.adapter.in.web;

import com.yapp.d14.portfolio.adapter.in.web.request.PortfolioRegisterHttpRequest;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioDeleteHttpResponse;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioFileUrlHttpResponse;
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
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "PROCESSING — 분석 중", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "PROCESSING",
                                                "message": "포트폴리오를 분석하고 있어요."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "READY — 처리 완료", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "READY",
                                                "message": "포트폴리오 처리가 완료되었습니다."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "FAILED_FILE — 파일 손상·암호 보호", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "FAILED_FILE",
                                                "message": "파일이 손상되었거나 암호로 보호되어 있어요. 다시 업로드해 주세요."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "FAILED_FILE — 텍스트 인식 불가(스캔본 등)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "FAILED_FILE",
                                                "message": "텍스트를 인식할 수 없어요. 스캔본이 아닌 PDF 파일로 다시 업로드해 주세요."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "FAILED_SYSTEM — S3 업로드 실패", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "FAILED_SYSTEM",
                                                "message": "파일 업로드에 실패했어요. 잠시 후 다시 시도해 주세요."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "FAILED_SYSTEM — 분석(임베딩) 실패", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "FAILED_SYSTEM",
                                                "message": "포트폴리오 분석에 실패했어요. 잠시 후 다시 시도해 주세요."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "FAILED_SYSTEM — 처리 시간 초과", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "FAILED_SYSTEM",
                                                "message": "처리 시간이 초과되었어요. 다시 시도해 주세요."
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "FAILED_SYSTEM — 비동기 처리 큐 포화(등록 직후 즉시 실패)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                "status": "FAILED_SYSTEM",
                                                "message": "현재 요청이 많아 처리가 지연되고 있어요. 잠시 후 다시 시도해 주세요."
                                              }
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
                    "- `replaceAvailable`: 이번 달 남은 재업로드(교체) 기회입니다. `true`면 1회 업로드 가능, " +
                    "`false`면 이미 이번 달 재업로드를 마쳐 업로드 시도 시 `REPLACEMENT_LIMIT_EXCEEDED`로 거부됩니다. " +
                    "`nextAvailableAt`: `false`일 때만 값이 채워지며 다시 가능해지는 시각(다음 달 1일 0시)을 나타냅니다.\n" +
                    "- `deleteAvailable`: 이번 달 남은 삭제 기회입니다. `replaceAvailable`과 독립적으로 집계되므로, " +
                    "재업로드 기회를 이미 썼어도 삭제 기회가 남아있으면 `true`이고, 반대로 삭제 기회를 이미 썼어도 재업로드 기회는 남아있을 수 있습니다. " +
                    "`false`면 삭제 시도 시 `DELETE_LIMIT_EXCEEDED`로 거부됩니다. " +
                    "`nextDeleteAvailableAt`: `false`일 때만 값이 채워지며 다시 가능해지는 시각(다음 달 1일 0시)을 나타냅니다.\n" +
                    "- `interviewInProgress`: 이 포트폴리오로 진행 중인 면접이 있으면 `true`이며, 이 경우 `deleteAvailable`과 무관하게 삭제 API 호출 시 " +
                    "`PORTFOLIO_DELETE_BLOCKED_BY_INTERVIEW`로 거부됩니다. `interviewInProgress`와 `deleteAvailable`을 함께 보면 " +
                    "삭제 불가 사유(면접 진행 중 vs 이번 달 삭제 기회 소진)를 구분해 안내 메시지를 다르게 보여줄 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "등록된 포트폴리오 없음", value = """
                                            {
                                              "success": true,
                                              "data": { "portfolios": [] }
                                            }
                                            """),
                                    @ExampleObject(name = "처리 중(PROCESSING)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolios": [{
                                                  "portfolioId": "b1f2c3d4-0000-0000-0000-000000000001",
                                                  "fileName": "portfolio.pdf",
                                                  "fileSize": 1048576,
                                                  "pageCount": 12,
                                                  "status": "PROCESSING",
                                                  "uploadedAt": null,
                                                  "replaceAvailable": true,
                                                  "nextAvailableAt": null,
                                                  "deleteAvailable": true,
                                                  "nextDeleteAvailableAt": null,
                                                  "interviewInProgress": false
                                                }]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "완료(READY) — 삭제·재업로드 기회 모두 있음", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolios": [{
                                                  "portfolioId": "b1f2c3d4-0000-0000-0000-000000000002",
                                                  "fileName": "portfolio.pdf",
                                                  "fileSize": 1048576,
                                                  "pageCount": 12,
                                                  "status": "READY",
                                                  "uploadedAt": "2026-07-01T10:00:00",
                                                  "replaceAvailable": true,
                                                  "nextAvailableAt": null,
                                                  "deleteAvailable": true,
                                                  "nextDeleteAvailableAt": null,
                                                  "interviewInProgress": false
                                                }]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "완료(READY) — 이번 달 삭제·재업로드 기회 모두 소진", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolios": [{
                                                  "portfolioId": "b1f2c3d4-0000-0000-0000-000000000003",
                                                  "fileName": "portfolio.pdf",
                                                  "fileSize": 1048576,
                                                  "pageCount": 12,
                                                  "status": "READY",
                                                  "uploadedAt": "2026-07-15T10:00:00",
                                                  "replaceAvailable": false,
                                                  "nextAvailableAt": "2026-08-01T00:00:00",
                                                  "deleteAvailable": false,
                                                  "nextDeleteAvailableAt": "2026-08-01T00:00:00",
                                                  "interviewInProgress": false
                                                }]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "완료(READY) — 재업로드 기회만 소진(삭제는 아직 가능, 서로 독립)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolios": [{
                                                  "portfolioId": "b1f2c3d4-0000-0000-0000-000000000006",
                                                  "fileName": "portfolio.pdf",
                                                  "fileSize": 1048576,
                                                  "pageCount": 12,
                                                  "status": "READY",
                                                  "uploadedAt": "2026-07-18T10:00:00",
                                                  "replaceAvailable": false,
                                                  "nextAvailableAt": "2026-08-01T00:00:00",
                                                  "deleteAvailable": true,
                                                  "nextDeleteAvailableAt": null,
                                                  "interviewInProgress": false
                                                }]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "완료(READY) — 진행 중인 면접이 사용 중이라 삭제 불가", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolios": [{
                                                  "portfolioId": "b1f2c3d4-0000-0000-0000-000000000004",
                                                  "fileName": "portfolio.pdf",
                                                  "fileSize": 1048576,
                                                  "pageCount": 12,
                                                  "status": "READY",
                                                  "uploadedAt": "2026-07-20T10:00:00",
                                                  "replaceAvailable": true,
                                                  "nextAvailableAt": null,
                                                  "deleteAvailable": true,
                                                  "nextDeleteAvailableAt": null,
                                                  "interviewInProgress": true
                                                }]
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "처리 실패(FAILED_FILE·FAILED_SYSTEM)", value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "portfolios": [{
                                                  "portfolioId": "b1f2c3d4-0000-0000-0000-000000000005",
                                                  "fileName": "portfolio.pdf",
                                                  "fileSize": 1048576,
                                                  "pageCount": 12,
                                                  "status": "FAILED_FILE",
                                                  "uploadedAt": null,
                                                  "replaceAvailable": true,
                                                  "nextAvailableAt": null,
                                                  "deleteAvailable": true,
                                                  "nextDeleteAvailableAt": null,
                                                  "interviewInProgress": false
                                                }]
                                              }
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<PortfolioListHttpResponse>> getList(
            @Parameter(hidden = true) @CurrentUser UUID userId
    );

    @Operation(
            summary = "포트폴리오 파일 열람 URL 발급",
            description = "포트폴리오 목록에서 항목을 클릭했을 때 PDF 원본을 열람할 수 있는 URL을 발급합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 응답의 `fileUrl`은 S3 presigned GET URL이며 발급 후 10분간만 유효합니다. 매번 새로 호출해 발급받아야 합니다.\n" +
                    "- `portfolioId`로 지정한 포트폴리오는 반드시 `READY` 상태여야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "아직 처리 중이거나 처리에 실패한 포트폴리오",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
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
    ResponseEntity<ApiResponse<PortfolioFileUrlHttpResponse>> getFileUrl(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "포트폴리오 ID") UUID portfolioId
    );

    @Operation(
            summary = "포트폴리오 삭제",
            description = "포트폴리오를 삭제합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})\n\n" +
                    "- 진행 중인 면접이 이 포트폴리오를 사용 중이면 삭제할 수 없습니다.\n" +
                    "- 이번 달 삭제 기회를 이미 사용했다면(목록 조회 응답의 `deleteAvailable=false`) 삭제할 수 없습니다. " +
                    "재업로드 기회(`replaceAvailable`)와는 독립적으로 집계되므로, 재업로드 기회 소진 여부와 무관하게 삭제 기회만으로 판단합니다."
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "진행 중인 면접이 사용 중이거나, 이번 달 삭제 횟수를 이미 사용해 삭제할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "진행 중인 면접이 사용 중", value = """
                                            {
                                              "success": false,
                                              "code": "PORTFOLIO_DELETE_BLOCKED_BY_INTERVIEW",
                                              "message": "진행 중인 면접이 있어 포트폴리오를 삭제할 수 없어요. 면접 종료 후 다시 시도해 주세요."
                                            }
                                            """),
                                    @ExampleObject(name = "이번 달 삭제 횟수 소진", value = """
                                            {
                                              "success": false,
                                              "code": "DELETE_LIMIT_EXCEEDED",
                                              "message": "포트폴리오 삭제는 한 달에 한 번만 가능해요. 다음 달 1일부터 다시 시도해 주세요."
                                            }
                                            """)
                            }
                    )
            )
    })
    ResponseEntity<ApiResponse<PortfolioDeleteHttpResponse>> delete(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(description = "포트폴리오 ID") UUID portfolioId
    );
}
