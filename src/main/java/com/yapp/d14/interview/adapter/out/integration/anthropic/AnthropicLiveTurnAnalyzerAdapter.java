package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.CeilingAssessment;
import com.yapp.d14.interview.application.port.out.LiveTurnAnalyzer;
import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.PriorQaCache;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.StaleProbeUpdate;
import com.yapp.d14.interview.domain.CeilingKind;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateStaleReason;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.portfolio.application.port.in.PortfolioChunkSearchUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.anthropic.api.AnthropicCacheOptions;
import org.springframework.ai.anthropic.api.AnthropicCacheStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
class AnthropicLiveTurnAnalyzerAdapter implements LiveTurnAnalyzer {

    private static final String AXES_YAML_PATH = "interview-rubric/axes.yaml";
    private static final String PRINCIPLES_YAML_PATH = "interview-rubric/principles.yaml";
    private static final String CEILING_FEWSHOT_PATH = "interview-rubric/ceiling-fewshot.md";
    // AnthropicProbeCandidateExtractorAdapter.MAX_CANDIDATES 와 동일한 취지 — 한 턴에서 뽑히는 캐물지점 후보가
    // 과도하게 쌓이는 것을 막는 상한이다.
    private static final int MAX_NEW_PROBES = 2;
    private static final int MAX_PROBE_TEXT_CHARS = 60;
    private static final int MAX_CEILING_REASON_CHARS = 40;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 AI 면접관입니다. 지원자의 방금 답변을 분석해 아래 세 가지를 판단합니다.
            1. 더 파고들어 물어볼 만한 "캐물지점" 후보 추출 (new_probes)
            2. current_axis가 주어졌다면, 그 항목에 대해 천장(ceiling)에 도달했는지 판별
            3. 방금 답변이 이전에 열려 있던 캐물지점의 전제와 모순되거나 스스로 정정하는지 감지 (stale_updates)

            아래 6대 평가 항목(axis) 정의를 기준으로 각 캐물지점에 axis 태그를 답니다.
            %s

            아래는 캐물지점을 만들 때 참고할 전술 목록(P1~P24)과 직군별 필수/권장 매트릭스입니다.
            사용자 메시지에 명시된 jobRole에 해당하는 job_role_profiles 항목을 확인해,
            REQUIRED로 표시된 P21~P24 전술을 우선 적용하고 그 다음 RECOMMENDED로 확장하세요.
            각 캐물지점에는 사용한 전술의 id(P1~P24)를 principleUsed에 기록하세요. 참고한 전술이 없으면 null입니다.
            %s

            아래는 천장 판별 기준을 잡아주는 예시입니다. current_axis가 없으면(첫 턴) 천장 판별은 하지 않습니다.
            %s

            new_probes 규칙:
            - 캐물지점은 반드시 방금 답변 내용에 근거해야 합니다.
            - probeText는 "무엇을 캐물을지"만 적는 내부 메모입니다. 한 항목에 캐물 각도를 하나만 담아,
              %d자 이내의 명사구 한 문장으로 씁니다. 물음표와 부연 설명은 쓰지 않습니다.
              캐물 각도가 여러 개 보이면 가장 강한 하나만 남기고, 나머지는 별개의 후보로 분리하세요.
              실제 질문 문장은 이후 단계에서 따로 생성하므로, 여기서 질문을 완성하면 그 작업은 버려집니다.
              예시(각각 하나의 각도만 담고 있습니다):
                "블루그린 배포에서 트래픽 전환 실패를 감지하고 롤백을 트리거하는 판단 기준"
                "write-back 캐시에서 정합성이 깨지는 조건과 이를 감지·복구하는 설계"
                "테스트 분리 전략을 바꾸게 된 판단 근거와 당시의 기술적 제약"
            - echoQuote는 질문할 때 그대로 되받아 물을 원 표현입니다.
            - strength는 반드시 high/mid/low 중 하나로만 답합니다(medium 등 다른 표현 금지).
              신호가 진할수록 high, 애매하면 mid, 약하면 low로 답니다.
            - 개수를 인위적으로 채우거나 줄이지 마세요. 답변에서 자연스럽게 나오는 만큼만 뽑되, 최대 %d개까지만 반환하세요.
              그보다 많이 나올 수 있다면 신호 강도(strength)가 가장 강한 순으로 추리세요.
            - 사용자 메시지의 [제외 axis] 목록에 있는 axis는 이미 끝났거나 이번 세션에서 아예 다루지 않는 항목이라
              더 이상 후보가 필요 없습니다. 그 axis로는 new_probes를 만들지 마세요.

            ceiling 규칙:
            - current_axis가 없으면 reached=false, kind=null, reason="current_axis 없음 - 판별 대상 아님"으로 고정합니다.
            - current_axis가 있으면, 방금 답변이 그 axis에 새 내용을 더했는지로 판단합니다.
            - 첫 답이 추상적이어도 곧장 천장으로 판정하지 말고, 구체화를 유도하는 재질문을 최소 한 번 던진 뒤에만 판정하세요.
              (즉 prior_qa에 같은 axis로 구체화를 시도한 이력이 없다면 아직 천장 판정을 내리지 마세요 — reached=false)
            - kind는 topped_out(위로 닿아 멈춤) 또는 stuck(못 올라가서 멈춤) 중 하나입니다.
            - reason은 판정 근거를 %d자 이내로 요약합니다. prior_qa 인용이나 답변 재서술 없이 결론만 적으세요.
              (예: "구체화 재질문 1회 필요 - 아직 수치·근거 부재 단계")

            stale_updates 규칙:
            - 사용자 메시지의 open_probes 목록에 있는 항목만 참조할 수 있습니다(probeId는 목록에 있는 값 그대로 사용).
            - 방금 답변이 open_probes 중 하나의 전제와 모순되면(지원자가 의식하지 못한 채 앞뒤가 어긋나면) reason=contradicted.
            - 방금 답변에서 지원자가 스스로 이전 발언을 정정하면 reason=corrected.
            - 모순·정정이 없으면 빈 배열을 반환합니다.

            tool 사용 규칙:
            - search_portfolio/read_project_detail: 포트폴리오는 프로젝트 단위로 구조화되어 있지 않고,
              텍스트 스니펫만 반환합니다. project_id나 title은 없습니다. 근거가 부족할 때만 사용하세요.
            - get_prior_qa: 사용자 메시지의 prior_qa는 이미 current_axis 이력입니다. 이 tool은 그 외의
              axis 이력이 추가로 필요할 때(교차 axis 모순 의심)만 사용하세요.

            출력은 다른 설명 없이 아래 스키마의 JSON 객체 하나만 반환하세요:
            {
              "newProbes": [{axis, secondaryAxis, probeText, echoQuote, jdMatch, strength, principleUsed}, ...],
              "ceiling": {reached, kind, reason},
              "staleUpdates": [{probeId, reason, flagRef}, ...]
            }
            """;

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final AnthropicChatOptions cachedChatOptions;
    private final PortfolioChunkSearchUseCase portfolioChunkSearchUseCase;
    private final PriorQaCache priorQaCache;

    AnthropicLiveTurnAnalyzerAdapter(
            @Qualifier("anthropicChatModel") ChatModel chatModel,
            PortfolioChunkSearchUseCase portfolioChunkSearchUseCase,
            PriorQaCache priorQaCache
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(
                loadResource(AXES_YAML_PATH), loadResource(PRINCIPLES_YAML_PATH), loadResource(CEILING_FEWSHOT_PATH),
                MAX_PROBE_TEXT_CHARS, MAX_NEW_PROBES, MAX_CEILING_REASON_CHARS
        );
        // 이 시스템 프롬프트(axes.yaml+principles.yaml+ceiling-fewshot.md)는 매 턴 동일하게 재사용되는데도
        // 캐싱 없이 매번 전체가 재처리되어 run_live_turn 지연시간의 절반 이상을 차지했다 — SYSTEM_ONLY로 캐싱한다.
        // AnthropicChatModelConfig가 구성한 기본 옵션(model/maxTokens/thinking=DISABLED)은 그대로 유지해야 하므로
        // 복사본에 cacheOptions만 얹는다.
        AnthropicChatOptions cachedOptions = AnthropicChatOptions.fromOptions((AnthropicChatOptions) chatModel.getDefaultOptions());
        cachedOptions.setCacheOptions(AnthropicCacheOptions.builder().strategy(AnthropicCacheStrategy.SYSTEM_ONLY).build());
        this.cachedChatOptions = cachedOptions;
        this.portfolioChunkSearchUseCase = portfolioChunkSearchUseCase;
        this.priorQaCache = priorQaCache;
    }

    @Override
    public LiveTurnResult analyze(
            Long sessionId,
            UUID portfolioId,
            String lastQuestion,
            String lastAnswer,
            TestType currentAxis,
            JobType jobRole,
            List<PriorTurn> priorQa,
            List<QuestionCandidate> openProbesForAxis,
            Set<TestType> exhaustedAxes
    ) {
        String userMessage = buildUserMessage(lastQuestion, lastAnswer, currentAxis, jobRole, priorQa, openProbesForAxis, exhaustedAxes);
        // 어댑터는 싱글턴이라 세션별 상태를 필드로 못 둔다 — tool 묶음은 호출마다 새로 만든다.
        LiveTurnTools tools = new LiveTurnTools(portfolioChunkSearchUseCase, priorQaCache, sessionId, portfolioId);

        try {
            ResponseEntity<ChatResponse, LiveTurnLlmResponse> responseEntity = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .tools(tools)
                    .options(cachedChatOptions)
                    .call()
                    .responseEntity(LiveTurnLlmResponse.class);
            logUsage(sessionId, responseEntity.response());
            LiveTurnLlmResponse response = responseEntity.entity();

            // 프롬프트로도 exhaustedAxes/상한을 지시하지만, 모델이 지키지 않을 수 있어 코드에서도 강제한다
            // (AnthropicProbeCandidateExtractorAdapter의 MAX_CANDIDATES와 동일한 원칙).
            List<ProbeCandidateDraft> newProbes = response.newProbes().stream()
                    .map(this::toDraft)
                    .filter(draft -> !exhaustedAxes.contains(draft.testType()))
                    .limit(MAX_NEW_PROBES)
                    .toList();
            CeilingAssessment ceiling = toCeilingAssessment(currentAxis, response.ceiling());
            List<StaleProbeUpdate> staleUpdates = toStaleUpdates(response.staleUpdates(), openProbesForAxis);

            return new LiveTurnResult(newProbes, ceiling, staleUpdates);
        } catch (Exception e) {
            log.error("[LIVE TURN ANALYZE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("답변 분석(run_live_turn)에 실패했어요.", e);
        }
    }

    // 캐시 토큰은 Spring AI 공용 Usage에 없고 Anthropic 네이티브 Usage에만 있다.
    private void logUsage(Long sessionId, ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        Integer cacheCreationInputTokens = null;
        Integer cacheReadInputTokens = null;
        if (usage.getNativeUsage() instanceof AnthropicApi.Usage nativeUsage) {
            cacheCreationInputTokens = nativeUsage.cacheCreationInputTokens();
            cacheReadInputTokens = nativeUsage.cacheReadInputTokens();
        }
        log.info(
                "[LIVE TURN USAGE] sessionId={}, inputTokens={}, outputTokens={}, cacheCreationInputTokens={}, cacheReadInputTokens={}",
                sessionId, usage.getPromptTokens(), usage.getCompletionTokens(),
                cacheCreationInputTokens, cacheReadInputTokens
        );
    }

    // 유저 메시지에 current_axis/jobRole/직전 질답과, prior_qa·open_probes·제외 axis(완료됐거나 예산 없는 axis) 컨텍스트를 채워넣는다.
    private String buildUserMessage(
            String lastQuestion,
            String lastAnswer,
            TestType currentAxis,
            JobType jobRole,
            List<PriorTurn> priorQa,
            List<QuestionCandidate> openProbesForAxis,
            Set<TestType> exhaustedAxes
    ) {
        String currentAxisText = currentAxis == null ? "없음 (첫 턴 요약 답변)" : currentAxis.name().toLowerCase();
        String priorQaText = priorQa.isEmpty() ? "없음" : priorQa.stream()
                .map(turn -> "- turn %d [%s] Q: %s / A: %s"
                        .formatted(turn.turnIndex(), turn.axis(), turn.question(), turn.answer()))
                .collect(Collectors.joining("\n"));
        String openProbesText = openProbesForAxis.isEmpty() ? "없음" : openProbesForAxis.stream()
                .map(probe -> "- probeId=%d probeText=%s echoQuote=%s"
                        .formatted(probe.getId(), probe.getProbeText(), probe.getEchoQuote()))
                .collect(Collectors.joining("\n"));
        String exhaustedAxesText = exhaustedAxes.isEmpty() ? "없음" : exhaustedAxes.stream()
                .map(axis -> axis.name().toLowerCase())
                .collect(Collectors.joining(", "));

        return """
                [직무] %s
                [current_axis] %s
                [제외 axis] %s
                [방금 질문] %s
                [방금 답변] %s
                [prior_qa]
                %s
                [open_probes]
                %s
                """.formatted(jobRole, currentAxisText, exhaustedAxesText, lastQuestion, lastAnswer, priorQaText, openProbesText);
    }

    // LLM 응답 1건을 ProbeCandidateDraft로 변환한다.
    private ProbeCandidateDraft toDraft(ProbeCandidateLlmEntry entry) {
        return new ProbeCandidateDraft(
                TestType.valueOf(entry.axis().toUpperCase()),
                StringUtils.hasText(entry.secondaryAxis()) ? TestType.valueOf(entry.secondaryAxis().toUpperCase()) : null,
                entry.probeText(),
                entry.echoQuote(),
                entry.jdMatch(),
                QuestionCandidateStrength.valueOf(entry.strength().toUpperCase()),
                entry.principleUsed()
        );
    }

    // currentAxis가 없으면 모델 출력과 무관하게 "판별 대상 아님"으로 고정해 반환한다.
    private CeilingAssessment toCeilingAssessment(TestType currentAxis, CeilingLlmEntry entry) {
        if (currentAxis == null) {
            return new CeilingAssessment(false, null, "current_axis 없음 - 판별 대상 아님");
        }
        if (entry == null) {
            throw new IllegalArgumentException("ceiling 응답이 누락됐어요.");
        }
        CeilingKind kind = StringUtils.hasText(entry.kind()) ? CeilingKind.valueOf(entry.kind().toUpperCase()) : null;
        return new CeilingAssessment(entry.reached(), kind, entry.reason());
    }

    // 모델이 open_probes에 없는 id를 지어낼 수 있으니, 실제로 넘겨준 id만 신뢰해 필터링 후 변환한다.
    // reason도 스키마(contradicted/corrected) 밖의 값을 반환할 수 있어, 알 수 없는 값은 해당 항목만 건너뛴다.
    static List<StaleProbeUpdate> toStaleUpdates(List<StaleUpdateLlmEntry> entries, List<QuestionCandidate> openProbesForAxis) {
        if (entries == null || entries.isEmpty() || openProbesForAxis.isEmpty()) {
            return List.of();
        }
        Set<Long> openProbeIds = openProbesForAxis.stream().map(QuestionCandidate::getId).collect(Collectors.toSet());
        return entries.stream()
                .filter(entry -> openProbeIds.contains(entry.probeId()))
                .map(AnthropicLiveTurnAnalyzerAdapter::toStaleUpdate)
                .filter(Objects::nonNull)
                .toList();
    }

    private static StaleProbeUpdate toStaleUpdate(StaleUpdateLlmEntry entry) {
        QuestionCandidateStaleReason reason = parseStaleReason(entry.reason());
        if (reason == null) {
            log.warn("[LIVE TURN ANALYZE] 알 수 없는 stale reason 값 - probeId={}, reason={}", entry.probeId(), entry.reason());
            return null;
        }
        return new StaleProbeUpdate(entry.probeId(), reason, entry.flagRef());
    }

    private static QuestionCandidateStaleReason parseStaleReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        try {
            return QuestionCandidateStaleReason.valueOf(reason.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 클래스패스 리소스를 문자열로 읽어온다.
    private static String loadResource(String path) {
        try {
            return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(path + " 로드에 실패했어요.", e);
        }
    }
}
