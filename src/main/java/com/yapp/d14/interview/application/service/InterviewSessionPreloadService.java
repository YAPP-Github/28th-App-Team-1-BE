package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionPreloadUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.JdKeywordExtractor;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.ProbeCandidateExtractor;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.portfolio.application.port.in.PortfolioChunkSearchUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioChunkResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewSessionPreloadService implements InterviewSessionPreloadUseCase {

    private static final int TOP_K = 20;
    private static final int MAX_LLM_RETRIES = 2;
    private static final int SUMMARY_TURN_LEVEL = 0;
    private static final int SUMMARY_DEPTH_LEVEL = 0;

    private final InterviewSessionRepository interviewSessionRepository;
    private final PortfolioChunkSearchUseCase portfolioChunkSearchUseCase;
    private final JdKeywordExtractor jdKeywordExtractor;
    private final ProbeCandidateExtractor probeCandidateExtractor;
    private final TextToSpeechSynthesizer textToSpeechSynthesizer;
    private final InterviewVoiceStorage interviewVoiceStorage;
    private final QuestionCandidateRepository questionCandidateRepository;
    private final QuestionRepository questionRepository;
    private final InterviewPreloadFailureHandler interviewPreloadFailureHandler;

    @Override
    @Async("interviewPreloadTaskExecutor")
    public void preload(Long sessionId) {
        log.info("interview preload async processing triggered: sessionId={}", sessionId);

        InterviewSession session = interviewSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("[INTERVIEW PRELOAD] 세션을 찾을 수 없어요: sessionId={}", sessionId);
            return;
        }

        try {
            List<String> chunks = searchPortfolioChunks(session);
            List<String> jdKeywords = extractJdKeywords(session);
            saveQuestionCandidates(session, chunks, jdKeywords);

            String questionText = buildSummaryQuestionText(session.getFocusProject());
            log.info("[INTERVIEW PRELOAD] 요약 질문 음성 합성 시작: sessionId={}", sessionId);
            byte[] audioContent = textToSpeechSynthesizer.synthesize(questionText);
            String aiVoiceS3Key = audioContent != null
                    ? interviewVoiceStorage.upload(session.getUserId(), sessionId, SUMMARY_TURN_LEVEL, audioContent)
                    : null;
            log.info("[INTERVIEW PRELOAD] 요약 질문 음성 처리 완료: sessionId={}, uploaded={}", sessionId, aiVoiceS3Key != null);

            questionRepository.save(Question.create(
                    sessionId, questionText, SUMMARY_TURN_LEVEL, SUMMARY_DEPTH_LEVEL, null, null, aiVoiceS3Key
            ));
            log.info("[INTERVIEW PRELOAD] 요약 질문 저장 완료: sessionId={}", sessionId);

            session.markReady();
            interviewSessionRepository.save(session);
            log.info("[INTERVIEW PRELOAD] 처리 완료, 세션 READY 전환: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("[INTERVIEW PRELOAD] 처리 실패: sessionId={}", sessionId, e);
            interviewPreloadFailureHandler.markFailed(sessionId);
        }
    }

    private List<String> searchPortfolioChunks(InterviewSession session) {
        String queryText = StringUtils.hasText(session.getFocusProject())
                ? session.getFocusProject()
                : session.getSnapshotJobType().getLabel() + " 프로젝트 경험";

        List<String> chunks = portfolioChunkSearchUseCase.searchChunks(session.getPortfolioId(), queryText, TOP_K).stream()
                .map(PortfolioChunkResult::text)
                .toList();
        log.info("[INTERVIEW PRELOAD] 포트폴리오 청크 조회 완료: sessionId={}, chunkCount={}", session.getId(), chunks.size());
        return chunks;
    }

    private List<String> extractJdKeywords(InterviewSession session) {
        String jdText = session.getJdText();
        if (!StringUtils.hasText(jdText)) {
            log.info("[INTERVIEW PRELOAD] JD 원문 없음, 키워드 추출 건너뜀: sessionId={}", session.getId());
            return List.of();
        }

        log.info("[INTERVIEW PRELOAD] JD 키워드 추출 시작: sessionId={}", session.getId());
        List<String> jdKeywords = callWithRetry(() -> jdKeywordExtractor.extractKeywords(jdText));
        log.info("[INTERVIEW PRELOAD] JD 키워드 추출 완료: sessionId={}, keywordCount={}", session.getId(), jdKeywords.size());
        return jdKeywords;
    }

    private void saveQuestionCandidates(InterviewSession session, List<String> chunks, List<String> jdKeywords) {
        log.info("[INTERVIEW PRELOAD] 캐물지점 추출 시작: sessionId={}, chunkCount={}, jdKeywordCount={}",
                session.getId(), chunks.size(), jdKeywords.size());
        List<ProbeCandidateDraft> drafts = callWithRetry(() -> probeCandidateExtractor.extract(chunks, jdKeywords));
        log.info("[INTERVIEW PRELOAD] 캐물지점 추출 완료: sessionId={}, candidateCount={}", session.getId(), drafts.size());

        for (ProbeCandidateDraft draft : drafts) {
            QuestionCandidateSource source = StringUtils.hasText(draft.jdMatch())
                    ? QuestionCandidateSource.JD
                    : QuestionCandidateSource.PORTFOLIO;

            questionCandidateRepository.save(QuestionCandidate.create(
                    session.getId(),
                    source,
                    null,
                    draft.testType(),
                    draft.secondaryTestType(),
                    draft.probeText(),
                    draft.echoQuote(),
                    draft.jdMatch(),
                    draft.strength()
            ));
        }
        log.info("[INTERVIEW PRELOAD] 캐물지점 저장 완료: sessionId={}, savedCount={}", session.getId(), drafts.size());
    }

    private String buildSummaryQuestionText(String focusProject) {
        if (StringUtils.hasText(focusProject)) {
            return "포트폴리오에 있는 프로젝트 중 [%s] 프로젝트에 대해 2분간 자유롭게 요약해서 설명해 주세요.".formatted(focusProject);
        }
        return "포트폴리오에서 가장 자신 있는 프로젝트를 골라 2분간 자유롭게 요약해서 설명해 주세요.";
    }

    private <T> T callWithRetry(Supplier<T> call) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_LLM_RETRIES + 1; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("[INTERVIEW PRELOAD] LLM 호출 실패 ({}/{})", attempt, MAX_LLM_RETRIES + 1, e);
            }
        }
        throw lastError;
    }
}
