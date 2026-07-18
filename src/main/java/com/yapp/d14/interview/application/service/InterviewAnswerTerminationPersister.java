package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.InterviewEndType;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.ticket.application.port.in.TicketCommitUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class InterviewAnswerTerminationPersister {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final TicketCommitUseCase ticketCommitUseCase;

    record PersistResult(Long answerId) {
    }

    @Transactional
    PersistResult persist(
            InterviewSession session,
            Question question,
            Answer answer,
            InterviewEndType endType,
            String outcomeReason
    ) {
        Long answerId = null;
        if (answer != null) {
            try {
                answerId = answerRepository.save(answer).getId();
            } catch (DataIntegrityViolationException e) {
                throw new InterviewException(InterviewErrorCode.ANSWER_ALREADY_SUBMITTED);
            }
        }
        questionRepository.save(question);

        session.markCompleted(endType);
        interviewSessionRepository.save(session);

        ticketCommitUseCase.commit(session.getId(), outcomeReason);

        return new PersistResult(answerId);
    }
}
