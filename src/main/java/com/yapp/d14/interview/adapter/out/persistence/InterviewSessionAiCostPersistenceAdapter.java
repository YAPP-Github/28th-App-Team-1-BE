package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewSessionAiCostJpaEntity;
import com.yapp.d14.interview.application.port.out.AiCostDelta;
import com.yapp.d14.interview.application.port.out.InterviewSessionAiCostRepository;
import com.yapp.d14.interview.domain.AiProvider;
import com.yapp.d14.interview.domain.InterviewSessionAiCost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class InterviewSessionAiCostPersistenceAdapter implements InterviewSessionAiCostRepository {

    private final InterviewSessionAiCostJpaRepository interviewSessionAiCostJpaRepository;

    @Override
    @Transactional
    public void accumulate(AiCostDelta delta) {
        boolean anthropic = delta.provider() == AiProvider.ANTHROPIC;
        LocalDateTime now = LocalDateTime.now();

        interviewSessionAiCostJpaRepository.accumulate(
                delta.sessionId(),
                anthropic ? delta.inputTokens() : 0L,
                anthropic ? delta.outputTokens() : 0L,
                anthropic ? delta.cacheWriteTokens() : 0L,
                anthropic ? delta.cacheReadTokens() : 0L,
                anthropic ? delta.costNanoUsd() : 0L,
                anthropic ? 0L : delta.inputTokens(),
                anthropic ? 0L : delta.outputTokens(),
                anthropic ? 0L : delta.ttsCharacters(),
                anthropic ? 0L : delta.sttDurationMillis(),
                anthropic ? 0L : delta.costNanoUsd(),
                delta.costNanoUsd(),
                now
        );
    }

    @Override
    public Optional<InterviewSessionAiCost> findBySessionId(Long sessionId) {
        return interviewSessionAiCostJpaRepository.findById(sessionId)
                .map(InterviewSessionAiCostJpaEntity::toDomain);
    }
}
