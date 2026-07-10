package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface InterviewSessionJpaRepository extends JpaRepository<InterviewSessionJpaEntity, Long> {

    List<InterviewSessionJpaEntity> findAllByUserId(UUID userId);
}
