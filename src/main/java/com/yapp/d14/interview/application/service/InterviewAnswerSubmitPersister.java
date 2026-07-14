package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// InterviewAnswerSubmitService(turnLevel=0 경로)의 원자적 저장 단계.
// InterviewPreloadResultPersister와 동일하게, 부수효과(STT·LLM 호출)는 서비스에서 끝내고 DB 반영만 여기서 트랜잭션으로 묶는다.
@Component
@RequiredArgsConstructor
class InterviewAnswerSubmitPersister {

    private final AnswerRepository answerRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;
    private final QuestionRepository questionRepository;

    record PersistResult(Answer answer, Question question) {
    }

    @Transactional
    PersistResult persist(
            Answer answer,
            List<QuestionCandidate> newProbeCandidates,
            QuestionCandidate selectedProbe,
            int nextTurnLevel,
            InterviewAxisPlan nextAxisPlan,
            Question nextQuestion
    ) {
        Answer savedAnswer = answerRepository.save(answer);

        questionCandidateRepository.saveAll(newProbeCandidates);

        if (selectedProbe != null) {
            selectedProbe.markUsed(nextTurnLevel);
            questionCandidateRepository.save(selectedProbe);
        }

        nextAxisPlan.incrementUsedCount();
        interviewAxisPlanRepository.save(nextAxisPlan);

        Question savedQuestion = questionRepository.save(nextQuestion);

        return new PersistResult(savedAnswer, savedQuestion);
    }
}
