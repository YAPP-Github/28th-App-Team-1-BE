package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.application.port.out.PriorQuestionReader;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class PriorQuestionPersistenceAdapter implements PriorQuestionReader {

    private static final int MAX_SESSIONS = 5;
    private static final int MAX_QUESTIONS = 40;

    // 사용자에게 질문이 실제로 노출된 세션만. PREPARING/IN_PROGRESS/INVALID/PRELOAD_FAILED는 제외한다.
    private static final List<InterviewSessionStatus> EXPOSED_STATUSES =
            List.of(InterviewSessionStatus.COMPLETED, InterviewSessionStatus.ABANDONED);

    private final InterviewSessionJpaRepository interviewSessionJpaRepository;
    private final QuestionJpaRepository questionJpaRepository;

    @Override
    public List<String> readRecentQuestions(UUID userId, Long excludeSessionId) {
        List<Long> sessionIds = interviewSessionJpaRepository.findRecentIdsByUserIdAndStatusIn(
                userId, EXPOSED_STATUSES, excludeSessionId, PageRequest.of(0, MAX_SESSIONS)
        );
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        return questionJpaRepository.findRecentContentsBySessionIdIn(sessionIds, PageRequest.of(0, MAX_QUESTIONS));
    }
}
