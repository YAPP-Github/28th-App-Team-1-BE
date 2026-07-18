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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
class AnthropicLiveTurnAnalyzerAdapter implements LiveTurnAnalyzer {

    private static final String AXES_YAML_PATH = "interview-rubric/axes.yaml";
    private static final String PRINCIPLES_YAML_PATH = "interview-rubric/principles.yaml";
    private static final String CEILING_FEWSHOT_PATH = "interview-rubric/ceiling-fewshot.md";

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
            - probeText는 "무엇을 캐물을지"에 대한 내부 메모(질문 문장 아님), echoQuote는 질문할 때 그대로 되받아 물을 원 표현입니다.
            - strength는 신호가 진할수록 high, 약하면 low로 답니다.
            - 개수를 인위적으로 채우거나 줄이지 마세요. 답변에서 자연스럽게 나오는 만큼만 뽑습니다.

            ceiling 규칙:
            - current_axis가 없으면 reached=false, kind=null, reason="current_axis 없음 - 판별 대상 아님"으로 고정합니다.
            - current_axis가 있으면, 방금 답변이 그 axis에 새 내용을 더했는지로 판단합니다.
            - 첫 답이 추상적이어도 곧장 천장으로 판정하지 말고, 구체화를 유도하는 재질문을 최소 한 번 던진 뒤에만 판정하세요.
              (즉 prior_qa에 같은 axis로 구체화를 시도한 이력이 없다면 아직 천장 판정을 내리지 마세요 — reached=false)
            - kind는 topped_out(위로 닿아 멈춤) 또는 stuck(못 올라가서 멈춤) 중 하나입니다.

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
    private final PortfolioChunkSearchUseCase portfolioChunkSearchUseCase;
    private final PriorQaCache priorQaCache;

    AnthropicLiveTurnAnalyzerAdapter(
            @Qualifier("anthropicChatModel") ChatModel chatModel,
            PortfolioChunkSearchUseCase portfolioChunkSearchUseCase,
            PriorQaCache priorQaCache
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(
                loadResource(AXES_YAML_PATH), loadResource(PRINCIPLES_YAML_PATH), loadResource(CEILING_FEWSHOT_PATH)
        );
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
            List<QuestionCandidate> openProbesForAxis
    ) {
        String userMessage = buildUserMessage(lastQuestion, lastAnswer, currentAxis, jobRole, priorQa, openProbesForAxis);
        // 어댑터는 싱글턴이라 세션별 상태를 필드로 못 둔다 — tool 묶음은 호출마다 새로 만든다.
        LiveTurnTools tools = new LiveTurnTools(portfolioChunkSearchUseCase, priorQaCache, sessionId, portfolioId);

        try {
            LiveTurnLlmResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .tools(tools)
                    .call()
                    .entity(LiveTurnLlmResponse.class);

            List<ProbeCandidateDraft> newProbes = response.newProbes().stream().map(this::toDraft).toList();
            CeilingAssessment ceiling = toCeilingAssessment(currentAxis, response.ceiling());
            List<StaleProbeUpdate> staleUpdates = toStaleUpdates(response.staleUpdates(), openProbesForAxis);

            return new LiveTurnResult(newProbes, ceiling, staleUpdates);
        } catch (Exception e) {
            log.error("[LIVE TURN ANALYZE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("답변 분석(run_live_turn)에 실패했어요.", e);
        }
    }

    // 유저 메시지에 current_axis/jobRole/직전 질답과, prior_qa·open_probes 컨텍스트를 채워넣는다.
    private String buildUserMessage(
            String lastQuestion,
            String lastAnswer,
            TestType currentAxis,
            JobType jobRole,
            List<PriorTurn> priorQa,
            List<QuestionCandidate> openProbesForAxis
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

        return """
                [직무] %s
                [current_axis] %s
                [방금 질문] %s
                [방금 답변] %s
                [prior_qa]
                %s
                [open_probes]
                %s
                """.formatted(jobRole, currentAxisText, lastQuestion, lastAnswer, priorQaText, openProbesText);
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
        if (currentAxis == null || entry == null) {
            return new CeilingAssessment(false, null, "current_axis 없음 - 판별 대상 아님");
        }
        CeilingKind kind = StringUtils.hasText(entry.kind()) ? CeilingKind.valueOf(entry.kind().toUpperCase()) : null;
        return new CeilingAssessment(entry.reached(), kind, entry.reason());
    }

    // 모델이 open_probes에 없는 id를 지어낼 수 있으니, 실제로 넘겨준 id만 신뢰해 필터링 후 변환한다.
    private List<StaleProbeUpdate> toStaleUpdates(List<StaleUpdateLlmEntry> entries, List<QuestionCandidate> openProbesForAxis) {
        if (entries == null || entries.isEmpty() || openProbesForAxis.isEmpty()) {
            return List.of();
        }
        Set<Long> openProbeIds = openProbesForAxis.stream().map(QuestionCandidate::getId).collect(Collectors.toSet());
        return entries.stream()
                .filter(entry -> openProbeIds.contains(entry.probeId()))
                .map(entry -> new StaleProbeUpdate(
                        entry.probeId(),
                        QuestionCandidateStaleReason.valueOf(entry.reason().toUpperCase()),
                        entry.flagRef()
                ))
                .toList();
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
