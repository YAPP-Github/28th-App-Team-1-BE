package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.InterviewAxisPlan;

import java.util.List;
import java.util.Optional;

public interface InterviewAxisPlanRepository {

    InterviewAxisPlan save(InterviewAxisPlan interviewAxisPlan);

    Optional<InterviewAxisPlan> findById(Long id);

    List<InterviewAxisPlan> findAllBySessionId(Long sessionId);
}
