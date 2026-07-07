package com.yapp.d14.job.adapter.in.web.response;

import com.yapp.d14.job.domain.Job;
import io.swagger.v3.oas.annotations.media.Schema;

public record JobHttpResponse(
        @Schema(description = "직무 ID", example = "1")
        int jobId,

        @Schema(description = "직무 Enum 값", example = "BACKEND")
        String jobRole,

        @Schema(description = "직무 한글 표시명", example = "백엔드")
        String label
) {

    public static JobHttpResponse from(Job job) {
        return new JobHttpResponse(job.getId(), job.name(), job.getLabel());
    }
}
