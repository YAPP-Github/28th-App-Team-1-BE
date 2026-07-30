package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.domain.JobType;

record InterviewSessionCreateContext(String portfolioFileName, JobType jobRole, int careerYears) {
}
