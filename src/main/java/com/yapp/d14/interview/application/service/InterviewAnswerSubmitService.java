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
        InterviewSession session = findOwnedSession(userId, command.sessionId());
        Question question = findOwnedQuestion(session, command.questionId());
        validateTurnLevelMatches(command, question);

        if (question.getTurnLevel().equals(SUMMARY_TURN_LEVEL)) {
            return handleFirstTurn(session, question, command);
        }
        return handleRegularTurn(session, question, command);
    }

    private InterviewSession findOwnedSession(UUID userId, Long sessionId) {
        return interviewSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND));
    }

    private Question findOwnedQuestion(InterviewSession session, Long questionId) {
        return questionRepository.findById(questionId)
                .filter(q -> q.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.QUESTION_NOT_FOUND));
    }

    // TODO: clientRequestId 기반 idempotency, endType×audio 정합성 검증 등 나머지 진입부 분기는
    //       이슈2(진입 처리)에서 구현한다. 여기서는 questionId 기준 실제 turnLevel과 요청값 대조만 수행한다.
    private void validateTurnLevelMatches(InterviewAnswerSubmitCommand command, Question question) {
        if (!question.getTurnLevel().equals(command.turnLevel())) {
            throw new InterviewException(InterviewErrorCode.UNSUPPORTED_TURN_LEVEL);
        }
    }

    // turnLevel=0(요약 답변) 특수 처리 경로
    private InterviewAnswerSubmitResult handleFirstTurn(
            InterviewSession session, Question summaryQuestion, InterviewAnswerSubmitCommand command
    ) {
        String sttText = speechToTextTranscriber.transcribe(command.audioContent()); // STT 변환
        LiveTurnResult liveTurnResult = analyzeFirstTurn(session, summaryQuestion, sttText); // 캐물지점 추출

        InterviewAxisPlan nextAxisPlan = selectFirstCoreAxisPlan(session); // 다음 axis 선택
        TestType nextAxis = nextAxisPlan.getTestType(); // axis 값 추출
        Optional<QuestionCandidate> selectedProbe = selectNextProbe(session.getId(), nextAxis); // 후보 선택
        String nextQuestionText = generateNextQuestionText(selectedProbe); // 질문 문장 생성

        int nextTurnLevel = SUMMARY_TURN_LEVEL + 1; // 다음 턴 번호
        Question nextQuestion = Question.create(
                session.getId(), nextQuestionText, nextTurnLevel, 0, nextAxis, null, null
        ); // 다음 질문 생성
        List<QuestionCandidate> newProbeCandidates = toQuestionCandidates(
                session.getId(), liveTurnResult, command.turnLevel()
        ); // 새 후보 변환
        Answer answer = buildAnswer(session, summaryQuestion, sttText, command); // 답변 생성

        InterviewAnswerSubmitPersister.PersistResult persisted = interviewAnswerSubmitPersister.persist(
                answer, newProbeCandidates, selectedProbe.orElse(null), nextTurnLevel, nextAxisPlan, nextQuestion
        ); // 한번에 DB 저장

        return buildResult(persisted, nextTurnLevel); // 결과 반환
    }

    // TODO: turnLevel≥1 일반 매 턴 루프 (설계 문서 5장, 다이어그램 0-2). 이슈2 이후에서 아래 구조로 구현한다.
    //
    // 5-0. 진입부 분기 (이슈2) — 아래 중 하나라도 해당하면 정상 루프를 타지 않고 즉시 응답
    //   ├─ endType=EARLY_EXIT      → (audio 있으면 STT만) → 즉시 종료(wrapUpMessage:null)     → commit(EARLY_EXIT)
    //   ├─ endType=MANUAL_END      → (audio 있으면 STT만) → 즉시 종료(짧은 멘트)              → commit(COMPLETED)
    //   ├─ endType=HARD_CAP        → (audio 있으면 STT만) → 10초 카운트다운 후 종료            → commit(COMPLETED)
    //   ├─ 직전 turn이 isWrapUp=true였던 응답 → (audio 있으면 STT만) → 종료(정식 마무리 멘트)  → commit(COMPLETED)
    //   ├─ isWrapUp이 이번 턴에 처음 true로 전환 → 정상 진행 (axis 선택만 마무리 경로로 강제)
    //   └─ 그 외 → 정상 진행 (아래로)
    //
    // 정상 진행 시:
    //   1. STT 변환 (Whisper-1) (이슈3)
    //   2. STT 누적 인식률 갱신 — 30% 초과 시 status=invalid, release(STT_RESET), 세션 종료 (이슈3)
    //   3. run_live_turn (Haiku) — last_question, last_answer, current_axis, prior_qa 입력
    //      → new_probes, ceiling, stale_updates 출력 (이슈3, 이슈1의 축소 버전을 천장 판별 활성화로 확장)
    //   4. probe_candidate_pool에 new_probes 병합, stale_updates 반영 (이슈3)
    //   5. select_next_axis — 천장 도달/budget 소진/위험 신호 예외 반영 (이슈4, 신규)
    //   6. select_next_probe (이슈4, 이슈1에서 만든 것 재사용)
    //   7. generate_question_text (이슈4, 이슈1에서 만든 것 재사용)
    //   8. probe 상태 갱신(status=used), axis_plan.used_count +1
    //   9. 응답 반환 (nextQuestion: {questionId, isLast, turn}) — TTS는 기다리지 않고 즉시 반환 (방법 2-1)
    //
    // 참고: clientRequestId idempotency, endType×audio 정합성 검증은 문서상 turnLevel 분기 이전(submit()) 단계라
    //       이 메서드가 아니라 submit()에 추가해야 한다.
    private InterviewAnswerSubmitResult handleRegularTurn(
            InterviewSession session, Question question, InterviewAnswerSubmitCommand command
    ) {
        throw new InterviewException(InterviewErrorCode.UNSUPPORTED_TURN_LEVEL);
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

    private Optional<QuestionCandidate> selectNextProbe(Long sessionId, TestType axis) {
        List<QuestionCandidate> openCandidates = questionCandidateRepository
                .findOpenBySessionIdAndTestType(sessionId, axis);
        return NextProbeSelector.select(openCandidates);
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

    private Answer buildAnswer(
            InterviewSession session, Question summaryQuestion, String sttText, InterviewAnswerSubmitCommand command
    ) {
        return Answer.create(
                session.getId(), summaryQuestion.getId(), sttText,
                command.answerStartSec(), command.answerEndSec(), command.answerDuration(),
                false, null, null, null, null, false, false, null
        );
    }

    private InterviewAnswerSubmitResult buildResult(
            InterviewAnswerSubmitPersister.PersistResult persisted, int nextTurnLevel
    ) {
        return new InterviewAnswerSubmitResult(
                persisted.answer().getId(),
                new InterviewAnswerSubmitResult.NextQuestion(persisted.question().getId(), false, nextTurnLevel, 0),
                null,
                null
        );
    }
}
