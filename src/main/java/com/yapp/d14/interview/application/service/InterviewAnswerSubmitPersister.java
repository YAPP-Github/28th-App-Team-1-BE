package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
            Question answeredQuestion,
            List<QuestionCandidate> newProbeCandidates,
            QuestionCandidate selectedProbe,
            int nextTurnLevel,
            InterviewAxisPlan nextAxisPlan,
            Question nextQuestion
    ) {
        Answer savedAnswer;
        try {
            savedAnswer = answerRepository.save(answer);
        } catch (DataIntegrityViolationException e) {
            // question_id unique 제약 위반 = 동시 요청이 먼저 답변을 저장함 (동시 재시도 차단)
            throw new InterviewException(InterviewErrorCode.ANSWER_ALREADY_SUBMITTED);
        }
        questionRepository.save(answeredQuestion);

        // selectedProbe가 이번 턴에 새로 추출된 후보라면 newProbeCandidates에도 같은 인스턴스가 들어있을 수 있다.
        // 아래에서 selectedProbe를 별도로 markUsed 후 저장하므로, 여기서는 제외하고 저장해 중복 삽입을 막는다.
        List<QuestionCandidate> candidatesToInsert = newProbeCandidates.stream()
                .filter(candidate -> candidate != selectedProbe)
                .toList();
        questionCandidateRepository.saveAll(candidatesToInsert);

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
