package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewAxisPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface InterviewAxisPlanJpaRepository extends JpaRepository<InterviewAxisPlanJpaEntity, Long> {

    List<InterviewAxisPlanJpaEntity> findAllByInterviewSessionId(Long interviewSessionId);
}
