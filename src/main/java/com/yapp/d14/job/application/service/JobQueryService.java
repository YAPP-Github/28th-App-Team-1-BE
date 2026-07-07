package com.yapp.d14.job.application.service;

import com.yapp.d14.job.application.port.in.JobQueryUseCase;
import com.yapp.d14.job.domain.Job;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class JobQueryService implements JobQueryUseCase {

    @Override
    public List<Job> findAll() {
        return List.of(Job.values());
    }
}
