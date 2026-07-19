package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.util.S3KeyGenerator;
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
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewAnswerSubmitService implements InterviewAnswerSubmitUseCase {

    private static final int SUMMARY_TURN_LEVEL = 0;
    private static final int UNUSUALLY_SPECIFIC_HIGH_PROBE_THRESHOLD = 2;
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
    private final InterviewAnswerAnalyzePersister interviewAnswerAnalyzePersister;
    private final InterviewSttResetPersister interviewSttResetPersister;
    private final PriorQaCache priorQaCache;
    private final InterviewReportGenerateUseCase interviewReportGenerateUseCase;
    private final InterviewReportFailureHandler interviewReportFailureHandler;
    private final TextToSpeechSynthesizer textToSpeechSynthesizer;
    private final InterviewVoiceStorage interviewVoiceStorage;

    @Override
    public InterviewAnswerSubmitResult submit(UUID userId, InterviewAnswerSubmitCommand command) {
        InterviewSession session = InterviewSessionAccessSupport.requireOwned(interviewSessionRepository, command.sessionId(), userId);
        if (session.getStatus() == InterviewSessionStatus.COMPLETED || session.getStatus() == InterviewSessionStatus.INVALID) {
            throw new InterviewException(InterviewErrorCode.SESSION_ALREADY_ENDED);
        }
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
        String sttText = speechToTextTranscriber.transcribe(command.audioContent()).text(); // STT 변환
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
                false,
                null,
                null
        );
    }

    private InterviewAnswerSubmitResult handleRegularTurn(
            InterviewSession session, Question question, InterviewAnswerSubmitCommand command
    ) {
        InterviewEndType terminationEndType = resolveTerminationEndType(question, command);
        if (terminationEndType != null) {
            return handleTermination(session, question, command, terminationEndType);
        }
        if (command.endType() == InterviewEndType.SKIP) {
            return handleSkippedTurn(session, question, command);
        }
        return handleAnalysisTurn(session, question, command);
    }

    // SKIP 턴(5-1/5-3장): STT·캐물지점 추출·모순 감지를 생략하고 "스킵됨" 컨텍스트만 기록한다.
    private InterviewAnswerSubmitResult handleSkippedTurn(
            InterviewSession session, Question question, InterviewAnswerSubmitCommand command
    ) {
        Answer answer;
        try {
            answer = Answer.create(
                    session.getId(), question.getId(), null,
                    command.answerStartSec(), command.answerEndSec(), command.answerDuration(),
                    true, null, null, null, null, false, false, question.getTestType()
            );
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_ANSWER_RANGE);
        }
        markQuestionPlayed(question, command);

        NextQuestionPlan plan = planNextQuestion(session, question, command, true, false, false, List.of(), null);
        InterviewAnswerAnalyzePersister.PersistResult persisted = interviewAnswerAnalyzePersister.persistSkipped(
                answer, question, plan.selectedProbe(), plan.nextTurnLevel(), plan.nextAxisPlan(), plan.completedAxisPlan(), plan.nextQuestion()
        );

        return buildNextQuestionResult(persisted, plan);
    }

    // 5-2~5-4장: STT 변환 → 누적 인식률 갱신(초과 시 STT_RESET) → run_live_turn 일반화 → 결과 영속화.
    private InterviewAnswerSubmitResult handleAnalysisTurn(
            InterviewSession session, Question question, InterviewAnswerSubmitCommand command
    ) {
        TranscriptionResult transcription = speechToTextTranscriber.transcribe(command.audioContent());
        session.recordSttSegments(transcription.failedSegmentCount(), transcription.totalSegmentCount());
        if (session.isSttFailureRateExceeded()) {
            return handleSttReset(session, question, command, transcription);
        }

        TestType currentAxis = question.getTestType();
        List<PriorTurn> priorQa = priorQaCache.get(session.getId(), currentAxis);
        List<QuestionCandidate> openProbesForAxis =
                questionCandidateRepository.findOpenBySessionIdAndTestType(session.getId(), currentAxis);

        LiveTurnResult liveTurnResult = liveTurnAnalyzer.analyze(
                session.getId(), session.getPortfolioId(), question.getContent(), transcription.text(),
                currentAxis, session.getSnapshotJobType(), priorQa, openProbesForAxis
        );
        List<QuestionCandidate> newProbeCandidates =
                toQuestionCandidates(session.getId(), liveTurnResult, question.getTurnLevel());
        boolean hasContradiction = liveTurnResult.staleUpdates().stream()
                .anyMatch(update -> update.reason() == QuestionCandidateStaleReason.CONTRADICTED);
        boolean isUnusuallySpecific = isUnusuallySpecific(liveTurnResult.newProbes());

        Answer answer;
        try {
            answer = Answer.create(
                    session.getId(), question.getId(), transcription.text(),
                    command.answerStartSec(), command.answerEndSec(), command.answerDuration(),
                    false, sttFailureRatio(transcription), null, null, null,
                    liveTurnResult.ceiling().reached(), hasContradiction, currentAxis
            );
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_ANSWER_RANGE);
        }
        markQuestionPlayed(question, command);

        NextQuestionPlan plan = planNextQuestion(
                session, question, command, liveTurnResult.ceiling().reached(), hasContradiction, isUnusuallySpecific,
                newProbeCandidates, openProbesForAxis
        );
        InterviewAnswerAnalyzePersister.PersistResult persisted = interviewAnswerAnalyzePersister.persist(
                session, answer, question, newProbeCandidates, liveTurnResult.staleUpdates(), question.getTurnLevel(),
                plan.selectedProbe(), plan.nextTurnLevel(), plan.nextAxisPlan(), plan.completedAxisPlan(), plan.nextQuestion()
        );
        appendPriorQaSafely(session.getId(), currentAxis, question, transcription.text());

        return buildNextQuestionResult(persisted, plan);
    }

    // 5-2/6-2장: 세션 누적 STT 인식 실패율이 30%를 초과하면 완전 리셋(무효화)하고 즉시 세션을 종료한다.
    private InterviewAnswerSubmitResult handleSttReset(
            InterviewSession session, Question question, InterviewAnswerSubmitCommand command, TranscriptionResult transcription
    ) {
        Answer answer;
        try {
            answer = Answer.create(
                    session.getId(), question.getId(), transcription.text(),
                    command.answerStartSec(), command.answerEndSec(), command.answerDuration(),
                    false, sttFailureRatio(transcription), null, null, null, false, false, question.getTestType()
            );
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_ANSWER_RANGE);
        }
        markQuestionPlayed(question, command);

        InterviewSttResetPersister.PersistResult persisted = interviewSttResetPersister.persist(session, question, answer);
        priorQaCache.clear(session.getId());

        return new InterviewAnswerSubmitResult(persisted.answerId(), null, true, null, null);
    }

    // 답변은 이미 커밋된 뒤라 캐시 추가 실패로 재시도 불가능한 요청 전체 실패를 만들지 않는다 — 로그만 남기고 삼킨다.
    private void appendPriorQaSafely(Long sessionId, TestType currentAxis, Question question, String answerText) {
        try {
            priorQaCache.append(
                    sessionId, currentAxis,
                    new PriorTurn(question.getTurnLevel(), question.getContent(), answerText, currentAxis)
            );
        } catch (Exception e) {
            log.error("[PRIOR QA CACHE] append 실패, 해당 턴은 캐시에서 누락됩니다: sessionId={}, axis={}", sessionId, currentAxis, e);
        }
    }

    private void markQuestionPlayed(Question question, InterviewAnswerSubmitCommand command) {
        try {
            question.markPlayed(command.questionAudioStartSec(), command.questionAudioEndSec());
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_PLAYBACK_RANGE);
        }
    }

    private Float sttFailureRatio(TranscriptionResult transcription) {
        if (transcription.totalSegmentCount() == 0) {
            return null;
        }
        return (float) transcription.failedSegmentCount() / transcription.totalSegmentCount();
    }

    private InterviewEndType resolveTerminationEndType(Question question, InterviewAnswerSubmitCommand command) {
        if (command.endType() == InterviewEndType.EARLY_EXIT
                || command.endType() == InterviewEndType.MANUAL_END
                || command.endType() == InterviewEndType.HARD_CAP) {
            return command.endType();
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
        markQuestionPlayed(question, command);

        InterviewAnswerSubmitResult.WrapUpMessage wrapUpMessage = wrapUpMessageFor(endType);

        String outcomeReason = endType == InterviewEndType.EARLY_EXIT ? "EARLY_EXIT" : "COMPLETED";
        InterviewAnswerTerminationPersister.PersistResult persisted =
                interviewAnswerTerminationPersister.persist(session, question, answer, endType, outcomeReason);
        priorQaCache.clear(session.getId());

        triggerReportGeneration(session.getId());

        return new InterviewAnswerSubmitResult(persisted.answerId(), null, true, wrapUpMessage, null);
    }

    private Answer buildTerminationAnswer(InterviewSession session, Question question, InterviewAnswerSubmitCommand command) {
        if (command.audioContent() == null) {
            return null;
        }
        String sttText = speechToTextTranscriber.transcribe(command.audioContent()).text();
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
            // (To Server) 이렇게 마지막 답변 처리까지 완료후 비동기로 report 생성 불러내면 될까요?
            interviewReportGenerateUseCase.generate(sessionId);
        } catch (RejectedExecutionException e) {
            log.error("[INTERVIEW REPORT] 비동기 처리 큐가 가득 찼어요: sessionId={}", sessionId, e);
            interviewReportFailureHandler.markFailed(sessionId);
        }
    }

    private InterviewAnswerSubmitResult.WrapUpMessage wrapUpMessageFor(InterviewEndType endType) {
        String text = wrapUpTextFor(endType);
        if (text == null) {
            return null;
        }
        return new InterviewAnswerSubmitResult.WrapUpMessage(resolveWrapUpAudioBase64(endType, text));
    }

    private String wrapUpTextFor(InterviewEndType endType) {
        return switch (endType) {
            case EARLY_EXIT -> null;
            case MANUAL_END -> MANUAL_END_MESSAGE;
            case HARD_CAP -> HARD_CAP_MESSAGE;
            case NORMAL_END -> NORMAL_END_MESSAGE;
            default -> null;
        };
    }

    private String resolveWrapUpAudioBase64(InterviewEndType endType, String text) {
        String key = S3KeyGenerator.wrapUpMessageKey(endType.name());
        String cached = interviewVoiceStorage.readBase64(key);
        if (cached != null) {
            return cached;
        }
        byte[] audioContent = textToSpeechSynthesizer.synthesize(text);
        interviewVoiceStorage.upload(key, audioContent);
        return Base64.getEncoder().encodeToString(audioContent);
    }

    private LiveTurnResult analyzeFirstTurn(InterviewSession session, Question summaryQuestion, String sttText) {
        return liveTurnAnalyzer.analyze(
                session.getId(),
                session.getPortfolioId(),
                summaryQuestion.getContent(),
                sttText,
                null,
                session.getSnapshotJobType(),
                List.of(),
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

    // 다음질문 계획
    private record NextQuestionPlan(
            InterviewAxisPlan nextAxisPlan,
            InterviewAxisPlan completedAxisPlan,
            QuestionCandidate selectedProbe,
            Question nextQuestion,
            int nextTurnLevel,
            boolean isWrapUpForced
    ) {
    }

    private NextQuestionPlan planNextQuestion(
            InterviewSession session,
            Question question,
            InterviewAnswerSubmitCommand command,
            boolean ceilingReached,
            boolean hasRedFlag,
            boolean isUnusuallySpecific,
            List<QuestionCandidate> newProbeCandidates,
            List<QuestionCandidate> openProbesForCurrentAxis
    ) {
        TestType currentAxis = question.getTestType(); // 현재 축
        boolean isWrapUpForced = Boolean.TRUE.equals(command.isWrapUp()); // 랩업 강제
        int nextTurnLevel = question.getTurnLevel() + 1; // 다음 턴

        List<InterviewAxisPlan> axisPlans = interviewAxisPlanRepository.findAllBySessionId(session.getId()); // 축 계획 조회
        TestType nextAxis = isWrapUpForced
                ? currentAxis // 축 유지
                : NextAxisSelector.select(axisPlans, session.getWeights(), currentAxis, ceilingReached, hasRedFlag, isUnusuallySpecific); // 축 결정

        InterviewAxisPlan nextAxisPlan = findAxisPlan(axisPlans, nextAxis);
        InterviewAxisPlan completedAxisPlan = null;
        if (nextAxis != currentAxis) {
            completedAxisPlan = findAxisPlan(axisPlans, currentAxis);
            completedAxisPlan.markCompleted(); // 전환 확정
        }

        List<QuestionCandidate> knownOpenProbes = nextAxis == currentAxis ? openProbesForCurrentAxis : null; // 중복조회 방지
        Optional<QuestionCandidate> selectedProbe = selectNextProbe(session.getId(), nextAxis, newProbeCandidates, knownOpenProbes); // 후보 선택
        String nextQuestionText = generateNextQuestionText(selectedProbe); // 질문 문장
        int nextDepthLevel = nextAxis == currentAxis ? question.getDepthLevel() + 1 : 1; // 깊이 계산
        Question nextQuestion = Question.create(
                session.getId(), nextQuestionText, nextTurnLevel, nextDepthLevel, nextAxis, null, null, isWrapUpForced
        ); // 질문 생성

        return new NextQuestionPlan(
                nextAxisPlan, completedAxisPlan, selectedProbe.orElse(null), nextQuestion, nextTurnLevel, isWrapUpForced
        );
    }

    // 응답 변환
    private InterviewAnswerSubmitResult buildNextQuestionResult(
            InterviewAnswerAnalyzePersister.PersistResult persisted, NextQuestionPlan plan
    ) {
        return new InterviewAnswerSubmitResult(
                persisted.answerId(),
                new InterviewAnswerSubmitResult.NextQuestion(
                        persisted.nextQuestion().getId(), plan.isWrapUpForced(), plan.nextTurnLevel(), persisted.nextQuestion().getDepthLevel()
                ),
                false,
                null,
                null
        );
    }

    // 축 찾기
    private InterviewAxisPlan findAxisPlan(List<InterviewAxisPlan> axisPlans, TestType testType) {
        return axisPlans.stream()
                .filter(plan -> plan.getTestType() == testType)
                .findFirst()
                .orElseThrow();
    }

    // 구체 판정
    private boolean isUnusuallySpecific(List<ProbeCandidateDraft> newProbes) {
        long highStrengthCount = newProbes.stream()
                .filter(probe -> probe.strength() == QuestionCandidateStrength.HIGH)
                .count();
        return highStrengthCount >= UNUSUALLY_SPECIFIC_HIGH_PROBE_THRESHOLD;
    }

    // 선택 축(axis)의 기존 OPEN 후보와, 이번 턴에 새로 추출된 후보 중 같은 축인 것을 병합해 한 번에 선택한다.
    private Optional<QuestionCandidate> selectNextProbe(Long sessionId, TestType axis, List<QuestionCandidate> newProbeCandidates) {
        return selectNextProbe(sessionId, axis, newProbeCandidates, null);
    }

    // knownOpenProbes가 있으면(같은 axis를 이미 조회해둔 경우) 재조회 없이 그대로 재사용한다.
    private Optional<QuestionCandidate> selectNextProbe(
            Long sessionId, TestType axis, List<QuestionCandidate> newProbeCandidates, List<QuestionCandidate> knownOpenProbes
    ) {
        List<QuestionCandidate> candidatePool = new ArrayList<>(
                knownOpenProbes != null ? knownOpenProbes : questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, axis)
        );
        newProbeCandidates.stream()
                .filter(candidate -> candidate.getTestType() == axis)
                .forEach(candidatePool::add);
        return NextProbeSelector.select(candidatePool);
    }

    private String generateNextQuestionText(Optional<QuestionCandidate> selectedProbe) {
        return selectedProbe
                .map(probe -> questionTextGenerator.generate(probe.getProbeText(), probe.getEchoQuote()))
                .orElse(SEED_QUESTION_TEXT); //TODO 여는 질문 로직 구현 예정
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
                draft.strength(),
                draft.principleUsed()
        );
    }
}
