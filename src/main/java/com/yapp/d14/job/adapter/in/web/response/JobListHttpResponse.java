package com.yapp.d14.job.adapter.in.web.response;

import com.yapp.d14.job.domain.Job;

import java.util.List;

public record JobListHttpResponse(
        List<JobHttpResponse> jobs
) {

    public static JobListHttpResponse from(List<Job> jobs) {
        return new JobListHttpResponse(jobs.stream().map(JobHttpResponse::from).toList());
    }
}
