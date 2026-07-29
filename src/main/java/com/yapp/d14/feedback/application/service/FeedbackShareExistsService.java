package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.FeedbackShareExistsUseCase;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class FeedbackShareExistsService implements FeedbackShareExistsUseCase {

    private final FeedbackShareRepository feedbackShareRepository;

    @Override
    public boolean existsForSession(Long sessionId) {
        return feedbackShareRepository.findBySessionId(sessionId).isPresent();
    }
}
