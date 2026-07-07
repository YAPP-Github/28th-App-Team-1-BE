package com.yapp.d14.job.adapter.in.web;

import com.yapp.d14.job.adapter.in.web.response.JobListHttpResponse;
import com.yapp.d14.job.application.port.in.JobQueryUseCase;
import com.yapp.d14.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
class JobController implements JobControllerDocs {

    private final JobQueryUseCase jobQueryUseCase;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<JobListHttpResponse>> getJobs() {
        return ResponseEntity.ok(ApiResponse.ok(JobListHttpResponse.from(jobQueryUseCase.findAll())));
    }
}
