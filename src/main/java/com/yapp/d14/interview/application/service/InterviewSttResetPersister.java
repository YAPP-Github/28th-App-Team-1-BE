package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 세션 누적 STT 인식 실패율이 30%를 초과했을 때(STT_RESET)의 원자적 저장 단계.
@Component
@RequiredArgsConstructor
class InterviewSttResetPersister {

    private static final String OUTCOME_STT_RESET = "STT_RESET";

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final TicketReleaseUseCase ticketReleaseUseCase;

    record PersistResult(Long answerId) {
    }

    @Transactional
    PersistResult persist(InterviewSession session, Question question, Answer answer) {
        Long answerId;
        try {
            answerId = answerRepository.save(answer).getId();
        } catch (DataIntegrityViolationException e) {
            throw new InterviewException(InterviewErrorCode.ANSWER_ALREADY_SUBMITTED);
        }
        questionRepository.save(question);

        session.markInvalid();
        interviewSessionRepository.save(session);

        // status=invalid 전환과 같은 트랜잭션에서 처리 — release 실패 시 트랜잭션 전체를 롤백한다(try/catch로 삼키지 않음).
        ticketReleaseUseCase.release(session.getId(), OUTCOME_STT_RESET);

        return new PersistResult(answerId);
    }
}
