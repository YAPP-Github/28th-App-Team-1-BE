package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewPreloadResultPersisterTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private QuestionCandidateRepository questionCandidateRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private InterviewPreloadResultPersister persister;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    private InterviewSession session() {
        return InterviewSession.of(
                1L, userId, portfolioId, null, JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.PREPARING, null, null, null,
                25, 20, 10, 20, 10, 15, 0, 0
        );
    }

    private QuestionCandidate candidate() {
        return QuestionCandidate.create(
                1L, QuestionCandidateSource.PORTFOLIO, null, TestType.DEPTH, null,
                "probe", "echo", null, QuestionCandidateStrength.HIGH, null
        );
    }

    @Test
    void 저장_전에_세션ID_기준으로_기존_후보와_질문을_정리한다() {
        InterviewSession session = session();
        Question summaryQuestion = Question.create(1L, "질문", 0, 0, null, null, null, false);

        persister.persist(session, List.of(candidate()), summaryQuestion);

        InOrder order = inOrder(questionCandidateRepository, questionRepository);
        order.verify(questionCandidateRepository).deleteBySessionId(1L);
        order.verify(questionRepository).deleteBySessionId(1L);
        order.verify(questionCandidateRepository).save(any());
        order.verify(questionRepository).save(summaryQuestion);
    }

    @Test
    void 후보와_질문을_저장하고_세션을_READY로_전환한다() {
        InterviewSession session = session();
        Question summaryQuestion = Question.create(1L, "질문", 0, 0, null, null, null, false);
        List<QuestionCandidate> candidates = List.of(candidate(), candidate());

        persister.persist(session, candidates, summaryQuestion);

        verify(questionCandidateRepository, times(2)).save(any());
        verify(questionRepository).save(summaryQuestion);

        ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
        verify(interviewSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
    }
}
