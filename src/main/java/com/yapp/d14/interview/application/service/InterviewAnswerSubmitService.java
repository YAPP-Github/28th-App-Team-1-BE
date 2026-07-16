package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import com.yapp.d14.interview.application.port.in.InterviewAnswerSubmitUseCase;
import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.out.*;
import com.yapp.d14.interview.domain.*;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewAnswerSubmitService implements InterviewAnswerSubmitUseCase {

    private static final int SUMMARY_TURN_LEVEL = 0;
    private static final String SEED_QUESTION_TEXT = "조금 더 구체적으로 설명해 주실 수 있을까요?";
    private static final String MANUAL_END_MESSAGE = "오늘 면접은 여기까지 하겠습니다. 수고하셨습니다.";
    private static final String HARD_CAP_MESSAGE = "면접 시간이 다 되어 곧 마무리하겠습니다. 잠시 후 종료됩니다.";
    private static final String NORMAL_END_MESSAGE = "수고하셨습니다. 오늘 면접은 여기까지입니다.";

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final AnswerRepository answerRepository;
    private final SpeechToTextTranscriber speechToTextTranscriber;
    private final LiveTurnAnalyzer liveTurnAnalyzer;
    private final QuestionTextGenerator questionTextGenerator;
    private final InterviewAnswerSubmitPersister interviewAnswerSubmitPersister;
    private final InterviewAnswerTerminationPersister interviewAnswerTerminationPersister;
    private final InterviewReportGenerateUseCase interviewReportGenerateUseCase;
    private final InterviewReportFailureHandler interviewReportFailureHandler;

    @Override
    public InterviewAnswerSubmitResult submit(UUID userId, InterviewAnswerSubmitCommand command) {
        InterviewSession session = InterviewSessionAccessSupport.requireOwned(interviewSessionRepository, command.sessionId(), userId);
        Question question = InterviewSessionAccessSupport.requireOwnedQuestion(questionRepository, command.questionId(), session);

        // 같은 질문에 답변이 이미 있으면 직렬 재시도로 간주 — STT·LLM 재실행 전에 차단.
        // 동시 요청으로 인한 경합은 InterviewAnswerSubmitPersister의 unique 제약 위반 처리로 막는다.
        if (answerRepository.findByQuestionId(question.getId()).isPresent()) {
            throw new InterviewException(InterviewErrorCode.ANSWER_ALREADY_SUBMITTED);
        }

        if (question.getTurnLevel().equals(SUMMARY_TURN_LEVEL)) {
            return handleFirstTurn(session, question, command);
        }
        return handleRegularTurn(session, question, command);
    }

    // turnLevel=0(요약 답변) 특수 처리 경로
    private InterviewAnswerSubmitResult handleFirstTurn(
            InterviewSession session, Question summaryQuestion, InterviewAnswerSubmitCommand command
    ) {
        String sttText = speechToTextTranscriber.transcribe(command.audioContent()); // STT 변환
        LiveTurnResult liveTurnResult = analyzeFirstTurn(session, summaryQuestion, sttText); // 캐물지점 추출
        List<QuestionCandidate> newProbeCandidates = toQuestionCandidates(
                session.getId(), liveTurnResult, summaryQuestion.getTurnLevel()
        ); // 새 후보 변환 — 축 선택 전에 만들어 이번 턴에 추출한 후보도 선택 대상에 포함시킨다

        InterviewAxisPlan nextAxisPlan = selectFirstCoreAxisPlan(session); // 다음 axis 선택
        TestType nextAxis = nextAxisPlan.getTestType(); // axis 값 추출
        Optional<QuestionCandidate> selectedProbe = selectNextProbe(session.getId(), nextAxis, newProbeCandidates); // 기존 OPEN 후보 + 신규 후보를 병합해 한 번만 선택
        String nextQuestionText = generateNextQuestionText(selectedProbe); // 질문 문장 생성

        int nextTurnLevel = SUMMARY_TURN_LEVEL + 1;
        Question nextQuestion = Question.create(
                session.getId(), nextQuestionText, nextTurnLevel, 1, nextAxis, null, null, false
        ); // 다음 질문 생성
        Answer answer;
        try {
            answer = Answer.create(
                    session.getId(), summaryQuestion.getId(), sttText,
                    command.answerStartSec(), command.answerEndSec(), command.answerDuration(),
                    false, null, null, null, null, false, false, null
            );
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_ANSWER_RANGE);
        }
        try {
            summaryQuestion.markPlayed(command.questionAudioStartSec(), command.questionAudioEndSec()); // 재생 구간 기록
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_PLAYBACK_RANGE);
        }

        InterviewAnswerSubmitPersister.PersistResult persisted = interviewAnswerSubmitPersister.persist(
                answer, summaryQuestion, newProbeCandidates, selectedProbe.orElse(null), nextTurnLevel, nextAxisPlan, nextQuestion
        );

        return new InterviewAnswerSubmitResult(
                persisted.answer().getId(),
                new InterviewAnswerSubmitResult.NextQuestion(
                        persisted.question().getId(), false, nextTurnLevel, persisted.question().getDepthLevel()
                ),
                null,
                null
        );
    }

    private InterviewAnswerSubmitResult handleRegularTurn(
            InterviewSession session, Question question, InterviewAnswerSubmitCommand command
    ) {
        InterviewEndType terminationEndType = resolveTerminationEndType(question, command);
        if (terminationEndType == null) {
            return null;
        }
        return handleTermination(session, question, command, terminationEndType);
    }

    private InterviewEndType resolveTerminationEndType(Question question, InterviewAnswerSubmitCommand command) {
        if (command.endType() == InterviewEndType.EARLY_EXIT) {
            return InterviewEndType.EARLY_EXIT;
        }
        if (command.endType() == InterviewEndType.MANUAL_END) {
            return InterviewEndType.MANUAL_END;
        }
        if (command.endType() == InterviewEndType.HARD_CAP) {
            return InterviewEndType.HARD_CAP;
        }
        if (Boolean.TRUE.equals(question.getIsWrapUp())) {
            return InterviewEndType.NORMAL_END;
        }
        return null;
    }

    private InterviewAnswerSubmitResult handleTermination(
            InterviewSession session, Question question, InterviewAnswerSubmitCommand command, InterviewEndType endType
    ) {
        Answer answer = buildTerminationAnswer(session, question, command);

        try {
            question.markPlayed(command.questionAudioStartSec(), command.questionAudioEndSec());
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_PLAYBACK_RANGE);
        }

        String outcomeReason = endType == InterviewEndType.EARLY_EXIT ? "EARLY_EXIT" : "COMPLETED";
        InterviewAnswerTerminationPersister.PersistResult persisted =
                interviewAnswerTerminationPersister.persist(session, question, answer, endType, outcomeReason);

        triggerReportGeneration(session.getId());

        return new InterviewAnswerSubmitResult(persisted.answerId(), null, wrapUpMessageFor(endType), null);
    }

    private Answer buildTerminationAnswer(InterviewSession session, Question question, InterviewAnswerSubmitCommand command) {
        if (command.audioContent() == null) {
            return null;
        }
        String sttText = speechToTextTranscriber.transcribe(command.audioContent());
        try {
            return Answer.create(
                    session.getId(), question.getId(), sttText,
                    command.answerStartSec(), command.answerEndSec(), command.answerDuration(),
                    false, null, null, null, null, false, false, null
            );
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_ANSWER_RANGE);
        }
    }

    private void triggerReportGeneration(Long sessionId) {
        try {
            interviewReportGenerateUseCase.generate(sessionId);
        } catch (RejectedExecutionException e) {
            log.error("[INTERVIEW REPORT] 비동기 처리 큐가 가득 찼어요: sessionId={}", sessionId, e);
            interviewReportFailureHandler.markFailed(sessionId);
        }
    }

    private Object wrapUpMessageFor(InterviewEndType endType) {
        return switch (endType) {
            case EARLY_EXIT -> null;
            case MANUAL_END -> MANUAL_END_MESSAGE;
            case HARD_CAP -> HARD_CAP_MESSAGE;
            case NORMAL_END -> NORMAL_END_MESSAGE;
            default -> null;
        };
    }

    private LiveTurnResult analyzeFirstTurn(InterviewSession session, Question summaryQuestion, String sttText) {
        return liveTurnAnalyzer.analyze(
                session.getId(),
                summaryQuestion.getContent(),
                sttText,
                null,
                session.getSnapshotJobType(),
                List.of()
        );
    }

    private InterviewAxisPlan selectFirstCoreAxisPlan(InterviewSession session) {
        List<InterviewAxisPlan> axisPlans = interviewAxisPlanRepository.findAllBySessionId(session.getId());
        TestType nextAxis = FirstCoreAxisSelector.select(axisPlans, session.getWeights())
                .orElseThrow(() -> new IllegalStateException("CORE tier 항목이 없어요. sessionId=" + session.getId()));
        return axisPlans.stream()
                .filter(plan -> plan.getTestType() == nextAxis)
                .findFirst()
                .orElseThrow();
    }

    // 선택 축(axis)의 기존 OPEN 후보와, 이번 턴에 새로 추출된 후보 중 같은 축인 것을 병합해 한 번에 선택한다.
    private Optional<QuestionCandidate> selectNextProbe(Long sessionId, TestType axis, List<QuestionCandidate> newProbeCandidates) {
        List<QuestionCandidate> candidatePool = new ArrayList<>(
                questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, axis)
        );
        newProbeCandidates.stream()
                .filter(candidate -> candidate.getTestType() == axis)
                .forEach(candidatePool::add);
        return NextProbeSelector.select(candidatePool);
    }

    private String generateNextQuestionText(Optional<QuestionCandidate> selectedProbe) {
        return selectedProbe
                .map(probe -> questionTextGenerator.generate(probe.getProbeText(), probe.getEchoQuote()))
                .orElse(SEED_QUESTION_TEXT);
    }

    private List<QuestionCandidate> toQuestionCandidates(Long sessionId, LiveTurnResult liveTurnResult, int turnLevel) {
        return liveTurnResult.newProbes().stream()
                .map(draft -> toQuestionCandidate(sessionId, draft, turnLevel))
                .toList();
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
