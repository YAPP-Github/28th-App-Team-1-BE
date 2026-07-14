package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.AxisReportScoreContext;
import com.yapp.d14.interview.application.port.out.AxisReportScorer;
import com.yapp.d14.interview.application.port.out.AxisScoreDraft;
import com.yapp.d14.interview.application.port.out.HeadlineContext;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.RedFlagReconcileContext;
import com.yapp.d14.interview.application.port.out.RedFlagReconciler;
import com.yapp.d14.interview.application.port.out.RedFlagVerdict;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext;
import com.yapp.d14.interview.application.port.out.ReportCardContentGenerator;
import com.yapp.d14.interview.application.port.out.ReportCardDraft;
import com.yapp.d14.interview.application.port.out.ReportHeadlineGenerator;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.CompositeScoreCalculator;
import com.yapp.d14.interview.domain.HeadlineBranch;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStaleReason;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.TestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewReportGenerateService implements InterviewReportGenerateUseCase {

    private static final int MAX_LLM_RETRIES = 2;
    private static final int SUMMARY_TURN_LEVEL = 0;
    private static final String INSUFFICIENT_ANALYSIS_HEADLINE =
            "이번 면접의 답변이 충분하지 않아요. 다음 면접 연습 때는 조금 더 충분한 답변을 말씀해주세요.";

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;
    private final AxisReportScorer axisReportScorer;
    private final RedFlagReconciler redFlagReconciler;
    private final ReportCardContentGenerator reportCardContentGenerator;
    private final ReportHeadlineGenerator reportHeadlineGenerator;
    private final InterviewReportPersister interviewReportPersister;
    private final InterviewReportFailureHandler interviewReportFailureHandler;

    private record Turn(
            int turnNumber,
            TestType testType,
            String questionContent,
            String answerText,
            boolean skipped,
            Float answerStartSec,
            Float answerEndSec
    ) {
    }

    @Override
    @Async("interviewReportTaskExecutor")
    public void generate(Long sessionId) {
        log.info("interview report generate async processing triggered: sessionId={}", sessionId);

        InterviewSession session = interviewSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("[INTERVIEW REPORT] 세션을 찾을 수 없어요: sessionId={}", sessionId);
            return;
        }

        try {
            List<Turn> turns = buildTurns(sessionId);
            if (turns.isEmpty()) {
                log.info("[INTERVIEW REPORT] 채점할 턴이 없음, 분석 부족으로 처리: sessionId={}", sessionId);
                Report report = Report.create(
                        sessionId, null, null, INSUFFICIENT_ANALYSIS_HEADLINE, HeadlineBranch.INSUFFICIENT_ANALYSIS, ReportStatus.INSUFFICIENT_ANALYSIS
                );
                interviewReportPersister.persist(sessionId, report, List.of(), List.of(), List.of());
                return;
            }

            List<QuestionCandidate> candidates = questionCandidateRepository.findAllBySessionId(sessionId);
            Map<TestType, Integer> weights = buildWeights(session);
            Set<TestType> coreAxes = buildCoreAxes(sessionId);

            List<AxisScoreDraft> axisScoreDrafts = scoreAxes(sessionId, turns);
            List<AxisEvaluation> axisEvaluations = toAxisEvaluations(sessionId, axisScoreDrafts);

            List<RedFlagVerdict> redFlagVerdicts = reconcileRedFlags(sessionId, candidates, turns);
            List<AxisEvaluation> cappedAxisEvaluations = applyCaps(axisEvaluations, redFlagVerdicts);
            boolean knockoutTriggered = redFlagVerdicts.stream().anyMatch(RedFlagVerdict::knockout);

            Optional<CompositeScoreCalculator.Result> result =
                    CompositeScoreCalculator.compute(cappedAxisEvaluations, weights, coreAxes, knockoutTriggered);
            boolean severeRedFlagPresent = redFlagVerdicts.stream().anyMatch(v -> v.type().isExposed());

            Report report = buildReport(sessionId, result, severeRedFlagPresent, cappedAxisEvaluations);
            List<RedFlag> redFlags = toRedFlags(sessionId, redFlagVerdicts);
            List<ReportCard> reportCards = generateReportCards(sessionId, cappedAxisEvaluations, turns);

            interviewReportPersister.persist(sessionId, report, cappedAxisEvaluations, redFlags, reportCards);
            log.info("[INTERVIEW REPORT] 처리 완료: sessionId={}, status={}", sessionId, report.getStatus());
        } catch (Exception e) {
            log.error("[INTERVIEW REPORT] 처리 실패: sessionId={}", sessionId, e);
            interviewReportFailureHandler.markFailed(sessionId);
        }
    }

    private List<Turn> buildTurns(Long sessionId) {
        List<Question> questions = questionRepository.findAllBySessionId(sessionId);
        Map<Long, Answer> answersByQuestionId = answerRepository.findAllBySessionId(sessionId).stream()
                .collect(Collectors.toMap(Answer::getQuestionId, answer -> answer));

        return questions.stream()
                .filter(question -> question.getTurnLevel() != null && question.getTurnLevel() > SUMMARY_TURN_LEVEL)
                .filter(question -> answersByQuestionId.containsKey(question.getId()))
                .sorted(Comparator.comparing(Question::getTurnLevel))
                .map(question -> {
                    Answer answer = answersByQuestionId.get(question.getId());
                    return new Turn(
                            question.getTurnLevel(),
                            question.getTestType(),
                            question.getContent(),
                            answer.getSttText(),
                            Boolean.TRUE.equals(answer.getIsSkipped()),
                            answer.getAnswerStartSec(),
                            answer.getAnswerEndSec()
                    );
                })
                .toList();
    }

    private Map<TestType, Integer> buildWeights(InterviewSession session) {
        Map<TestType, Integer> weights = new EnumMap<>(TestType.class);
        weights.put(TestType.DEPTH, session.getWeightDepth());
        weights.put(TestType.BOUNDARY, session.getWeightBoundary());
        weights.put(TestType.CONNECTION, session.getWeightConnection());
        weights.put(TestType.TRADEOFF, session.getWeightTradeoff());
        weights.put(TestType.CONFLICT, session.getWeightConflict());
        weights.put(TestType.RESILIENCE, session.getWeightResilience());
        return weights;
    }

    private Set<TestType> buildCoreAxes(Long sessionId) {
        return interviewAxisPlanRepository.findAllBySessionId(sessionId).stream()
                .filter(plan -> plan.getTier() == AxisTier.CORE)
                .map(InterviewAxisPlan::getTestType)
                .collect(Collectors.toSet());
    }

    private List<AxisScoreDraft> scoreAxes(Long sessionId, List<Turn> turns) {
        Map<TestType, List<Turn>> turnsByAxis = turns.stream()
                .collect(Collectors.groupingBy(Turn::testType, java.util.LinkedHashMap::new, Collectors.toList()));

        List<AxisReportScoreContext.AxisTurnGroup> axisTurnGroups = turnsByAxis.entrySet().stream()
                .map(entry -> new AxisReportScoreContext.AxisTurnGroup(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(turn -> new AxisReportScoreContext.Turn(
                                        turn.questionContent(), turn.answerText(), turn.skipped(), turn.answerStartSec(), turn.answerEndSec()
                                ))
                                .toList()
                ))
                .toList();

        log.info("[INTERVIEW REPORT] axis 채점 시작: sessionId={}, axisCount={}", sessionId, axisTurnGroups.size());
        List<AxisScoreDraft> drafts = callWithRetry(() -> axisReportScorer.score(new AxisReportScoreContext(axisTurnGroups)));
        log.info("[INTERVIEW REPORT] axis 채점 완료: sessionId={}, scoredCount={}", sessionId, drafts.size());
        return drafts;
    }

    private List<AxisEvaluation> toAxisEvaluations(Long sessionId, List<AxisScoreDraft> drafts) {
        return drafts.stream()
                .map(draft -> AxisEvaluation.create(
                        sessionId, draft.testType(), draft.score(), draft.resolutionLevel(),
                        draft.resolutionLowReason(), draft.evidenceTimestamps(), draft.rationale()
                ))
                .toList();
    }

    private List<RedFlagVerdict> reconcileRedFlags(Long sessionId, List<QuestionCandidate> candidates, List<Turn> turns) {
        List<RedFlagReconcileContext.PortfolioCandidate> portfolioCandidates = candidates.stream()
                .filter(candidate -> candidate.getSource() == QuestionCandidateSource.PORTFOLIO)
                .filter(candidate -> candidate.getUsedInTurn() != null)
                .map(candidate -> new RedFlagReconcileContext.PortfolioCandidate(
                        candidate.getTestType(), candidate.getUsedInTurn(), candidate.getProbeText(), candidate.getEchoQuote()
                ))
                .toList();

        List<RedFlagReconcileContext.ContradictionCandidate> contradictionCandidates = candidates.stream()
                .filter(candidate -> candidate.getStatus() == QuestionCandidateStatus.STALE
                        && candidate.getStaleReason() == QuestionCandidateStaleReason.CONTRADICTED)
                .map(candidate -> new RedFlagReconcileContext.ContradictionCandidate(
                        candidate.getTestType(), candidate.getEchoQuote(), candidate.getProbeText(),
                        parseTurnNumber(candidate.getSourceRef()), candidate.getContradictingTurnNumber()
                ))
                .toList();

        if (portfolioCandidates.isEmpty() && contradictionCandidates.isEmpty()) {
            log.info("[INTERVIEW REPORT] 레드플래그 후보 없음, 확정 스킵: sessionId={}", sessionId);
            return List.of();
        }

        List<RedFlagReconcileContext.Turn> contextTurns = turns.stream()
                .map(turn -> new RedFlagReconcileContext.Turn(
                        turn.turnNumber(), turn.testType(), turn.questionContent(), turn.answerText(),
                        turn.skipped(), turn.answerStartSec(), turn.answerEndSec()
                ))
                .toList();

        log.info("[INTERVIEW REPORT] 레드플래그 확정 시작: sessionId={}, portfolioCandidateCount={}, contradictionCandidateCount={}",
                sessionId, portfolioCandidates.size(), contradictionCandidates.size());
        List<RedFlagVerdict> verdicts = callWithRetry(() -> redFlagReconciler.reconcile(
                new RedFlagReconcileContext(portfolioCandidates, contradictionCandidates, contextTurns)
        ));
        log.info("[INTERVIEW REPORT] 레드플래그 확정 완료: sessionId={}, verdictCount={}", sessionId, verdicts.size());
        return verdicts;
    }

    private Integer parseTurnNumber(String sourceRef) {
        if (sourceRef == null) {
            return null;
        }
        String[] parts = sourceRef.trim().split("\\s+");
        try {
            return Integer.valueOf(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<AxisEvaluation> applyCaps(List<AxisEvaluation> axisEvaluations, List<RedFlagVerdict> verdicts) {
        Map<TestType, Integer> capsByAxis = new EnumMap<>(TestType.class);
        for (RedFlagVerdict verdict : verdicts) {
            if (verdict.affectedTestType() == null || verdict.capValue() == null) {
                continue;
            }
            capsByAxis.merge(verdict.affectedTestType(), verdict.capValue(), Math::min);
        }
        if (capsByAxis.isEmpty()) {
            return axisEvaluations;
        }
        return axisEvaluations.stream()
                .map(axisEvaluation -> {
                    Integer cap = capsByAxis.get(axisEvaluation.getTestType());
                    return cap == null ? axisEvaluation : axisEvaluation.withAppliedCap(cap);
                })
                .toList();
    }

    private Report buildReport(
            Long sessionId,
            Optional<CompositeScoreCalculator.Result> result,
            boolean severeRedFlagPresent,
            List<AxisEvaluation> axisEvaluations
    ) {
        if (result.isEmpty()) {
            return Report.create(
                    sessionId, null, null, INSUFFICIENT_ANALYSIS_HEADLINE, HeadlineBranch.INSUFFICIENT_ANALYSIS, ReportStatus.INSUFFICIENT_ANALYSIS
            );
        }

        HeadlineBranch branch = severeRedFlagPresent ? HeadlineBranch.SEVERE_RED_FLAG : HeadlineBranch.NORMAL;
        String headline = generateHeadline(sessionId, severeRedFlagPresent, axisEvaluations);

        return Report.create(
                sessionId, result.get().compositeScore(), result.get().grade(), headline, branch, ReportStatus.READY
        );
    }

    private String generateHeadline(Long sessionId, boolean severeRedFlagPresent, List<AxisEvaluation> axisEvaluations) {
        List<HeadlineContext.AxisTopic> axisTopics = axisEvaluations.stream()
                .map(axisEvaluation -> new HeadlineContext.AxisTopic(
                        axisEvaluation.getTestType(), axisEvaluation.getRationale(), axisEvaluation.getResolutionLevel()
                ))
                .toList();

        log.info("[INTERVIEW REPORT] 한 줄 요약 생성 시작: sessionId={}, severeRedFlagPresent={}", sessionId, severeRedFlagPresent);
        String headline = callWithRetry(() -> reportHeadlineGenerator.generate(new HeadlineContext(severeRedFlagPresent, axisTopics)));
        log.info("[INTERVIEW REPORT] 한 줄 요약 생성 완료: sessionId={}", sessionId);
        return headline;
    }

    private List<RedFlag> toRedFlags(Long sessionId, List<RedFlagVerdict> verdicts) {
        return verdicts.stream()
                .map(verdict -> RedFlag.create(
                        sessionId, verdict.type(), verdict.affectedTestType(), verdict.capValue(), verdict.knockout(), verdict.evidenceTimestamps()
                ))
                .toList();
    }

    private List<ReportCard> generateReportCards(Long sessionId, List<AxisEvaluation> axisEvaluations, List<Turn> turns) {
        if (axisEvaluations.isEmpty()) {
            return List.of();
        }

        Map<TestType, List<Turn>> turnsByAxis = turns.stream().collect(Collectors.groupingBy(Turn::testType));

        List<ReportCardContentContext.AxisCardInput> axisCards = axisEvaluations.stream()
                .map(axisEvaluation -> new ReportCardContentContext.AxisCardInput(
                        axisEvaluation.getTestType(),
                        turnsByAxis.getOrDefault(axisEvaluation.getTestType(), List.of()).stream()
                                .map(turn -> new ReportCardContentContext.Turn(
                                        turn.questionContent(), turn.answerText(), turn.skipped(), turn.answerStartSec(), turn.answerEndSec()
                                ))
                                .toList(),
                        axisEvaluation.getRationale(),
                        axisEvaluation.getResolutionLevel(),
                        axisEvaluation.getResolutionLowReason()
                ))
                .toList();

        log.info("[INTERVIEW REPORT] 리포트 카드 생성 시작: sessionId={}, cardCount={}", sessionId, axisCards.size());
        List<ReportCardDraft> drafts = callWithRetry(() -> reportCardContentGenerator.generate(new ReportCardContentContext(axisCards)));
        log.info("[INTERVIEW REPORT] 리포트 카드 생성 완료: sessionId={}", sessionId);

        return drafts.stream()
                .map(draft -> ReportCard.create(
                        sessionId, draft.testType(), draft.questionIntentTranslation(),
                        draft.highlightSpans(), draft.actionKeywords(), draft.rewriteSuggestion()
                ))
                .toList();
    }

    private <T> T callWithRetry(Supplier<T> call) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_LLM_RETRIES + 1; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("[INTERVIEW REPORT] LLM 호출 실패 ({}/{})", attempt, MAX_LLM_RETRIES + 1, e);
            }
        }
        throw lastError;
    }
}
