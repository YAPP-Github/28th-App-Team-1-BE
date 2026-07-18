package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewSessionOwnershipCheckService implements InterviewSessionOwnershipCheckUseCase {

    private final InterviewSessionRepository interviewSessionRepository;

    @Override
    public void requireOwned(UUID userId, Long sessionId) {
        InterviewSessionAccessSupport.requireOwned(interviewSessionRepository, sessionId, userId);
    }
}
