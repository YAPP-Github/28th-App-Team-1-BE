package com.yapp.d14.job.adapter.in.web;

import com.yapp.d14.job.adapter.in.web.response.JobListHttpResponse;
import com.yapp.d14.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Job", description = "직무 API")
public interface JobControllerDocs {

    @Operation(
            summary = "직무 목록 조회",
            description = "선택 가능한 직무 목록을 조회합니다.\n\n" +
                    "**인증**: Access Token 필요 (Authorization: Bearer {accessToken})"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = JobListHttpResponse.class))
            )
    })
    ResponseEntity<ApiResponse<JobListHttpResponse>> getJobs();
}
