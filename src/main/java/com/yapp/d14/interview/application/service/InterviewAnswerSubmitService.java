package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import com.yapp.d14.interview.application.port.in.InterviewAnswerSubmitUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.LiveTurnAnalyzer;
import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.QuestionTextGenerator;
import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.FirstCoreAxisSelector;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.NextProbeSelector;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 답변 제출(POST /answers) 중 turnLevel=0(첫 턴) 특수 처리 경로만 다룬다 (설계 문서 4-2, 0-3장).
// turnLevel≥1 일반 매 턴 루프, endType×audio 정합성 검증 등 진입부 분기는 이슈2 이후에서 구현한다.
@Service
@RequiredArgsConstructor
class InterviewAnswerSubmitService implements InterviewAnswerSubmitUseCase {

    private static final int SUMMARY_TURN_LEVEL = 0;
    // TODO: seed 질문 문구는 설계 문서 7장 미확정 사항(axis별 고정 문구 vs 즉석 생성). 우선 범용 문구로 대체.
    private static final String SEED_QUESTION_TEXT = "조금 더 구체적으로 설명해 주실 수 있을까요?";

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final SpeechToTextTranscriber speechToTextTranscriber;
    private final LiveTurnAnalyzer liveTurnAnalyzer;
    private final QuestionTextGenerator questionTextGenerator;
    private final InterviewAnswerSubmitPersister interviewAnswerSubmitPersister;

    @Override
    public InterviewAnswerSubmitResult submit(UUID userId, InterviewAnswerSubmitCommand command) {
        InterviewSession session = interviewSessionRepository.findById(command.sessionId())
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        Question summaryQuestion = questionRepository.findById(command.questionId())
                .filter(q -> q.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.QUESTION_NOT_FOUND));

        // TODO: questionId 기준 실제 turnLevel과 요청값을 대조하는 공통 검증(clientRequestId·endType×audio 포함)은
        //       이슈2(진입 처리)에서 구현한다. 여기서는 turnLevel=0 케이스만 최소한으로 가드한다.
        if (command.turnLevel() != SUMMARY_TURN_LEVEL || !summaryQuestion.getTurnLevel().equals(SUMMARY_TURN_LEVEL)) {
            throw new InterviewException(InterviewErrorCode.UNSUPPORTED_TURN_LEVEL);
        }

        String sttText = speechToTextTranscriber.transcribe(command.audioContent());

        LiveTurnResult liveTurnResult = liveTurnAnalyzer.analyze(
                session.getId(),
                summaryQuestion.getContent(),
                sttText,
                null,
                session.getSnapshotJobType(),
                List.of()
        );

        List<InterviewAxisPlan> axisPlans = interviewAxisPlanRepository.findAllBySessionId(session.getId());
        TestType nextAxis = FirstCoreAxisSelector.select(axisPlans, session.getWeights())
                .orElseThrow(() -> new IllegalStateException("CORE tier 항목이 없어요. sessionId=" + session.getId()));
        InterviewAxisPlan nextAxisPlan = axisPlans.stream()
                .filter(plan -> plan.getTestType() == nextAxis)
                .findFirst()
                .orElseThrow();

        List<QuestionCandidate> openCandidates = questionCandidateRepository
                .findOpenBySessionIdAndTestType(session.getId(), nextAxis);
        Optional<QuestionCandidate> selectedProbe = NextProbeSelector.select(openCandidates);

        String nextQuestionText = selectedProbe
                .map(probe -> questionTextGenerator.generate(probe.getProbeText(), probe.getEchoQuote()))
                .orElse(SEED_QUESTION_TEXT);

        int nextTurnLevel = SUMMARY_TURN_LEVEL + 1;
        Question nextQuestion = Question.create(
                session.getId(), nextQuestionText, nextTurnLevel, 0, nextAxis, null, null
        );

        List<QuestionCandidate> newProbeCandidates = liveTurnResult.newProbes().stream()
                .map(draft -> toQuestionCandidate(session.getId(), draft, command.turnLevel()))
                .toList();

        Answer answer = Answer.create(
                session.getId(), summaryQuestion.getId(), sttText,
                command.answerStartSec(), command.answerEndSec(), command.answerDuration(),
                false, null, null, null, null, false, false, null
        );

        InterviewAnswerSubmitPersister.PersistResult persisted = interviewAnswerSubmitPersister.persist(
                answer, newProbeCandidates, selectedProbe.orElse(null), nextTurnLevel, nextAxisPlan, nextQuestion
        );

        return new InterviewAnswerSubmitResult(
                persisted.answer().getId(),
                new InterviewAnswerSubmitResult.NextQuestion(persisted.question().getId(), false, nextTurnLevel, 0),
                null,
                null
        );
    }

    private QuestionCandidate toQuestionCandidate(Long sessionId, ProbeCandidateDraft draft, int turnLevel) {
        return QuestionCandidate.create(
                sessionId,
                QuestionCandidateSource.ANSWER,
                "턴 %d".formatted(turnLevel),
                draft.testType(),
                draft.secondaryTestType(),
                draft.probeText(),
                draft.echoQuote(),
                draft.jdMatch(),
                draft.strength()
        );
    }
}
