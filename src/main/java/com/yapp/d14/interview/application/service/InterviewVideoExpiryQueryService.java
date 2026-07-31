package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoExpiryQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoExpiryResult;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewVideoExpiryQueryService implements InterviewVideoExpiryQueryUseCase {

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewVideoRepository interviewVideoRepository;

    @Override
    public InterviewVideoExpiryResult getExpiry(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);
        InterviewVideo video = interviewVideoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_VIDEO_NOT_FOUND));

        // 폴링 중 만료 시각을 막 넘긴 순간에도 음수를 내려보내지 않도록 0으로 클램프한다.
        long remaining = Duration.between(LocalDateTime.now(), video.getExpiresAt()).getSeconds();
        return new InterviewVideoExpiryResult(Math.max(0L, remaining), video.isExpired());
    }
}
