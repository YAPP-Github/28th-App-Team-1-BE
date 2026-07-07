package com.yapp.d14.job.application.port.in;

import com.yapp.d14.job.domain.Job;

import java.util.List;

public interface JobQueryUseCase {

    List<Job> findAll();
}
