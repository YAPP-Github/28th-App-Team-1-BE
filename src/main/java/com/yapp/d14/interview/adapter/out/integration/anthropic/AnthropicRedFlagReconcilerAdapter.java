package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.RedFlagReconcileContext;
import com.yapp.d14.interview.application.port.out.RedFlagReconcileContext.ContradictionCandidate;
import com.yapp.d14.interview.application.port.out.RedFlagReconcileContext.PortfolioCandidate;
import com.yapp.d14.interview.application.port.out.RedFlagReconcileContext.Turn;
import com.yapp.d14.interview.application.port.out.RedFlagReconciler;
import com.yapp.d14.interview.application.port.out.RedFlagVerdict;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TimeRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
class AnthropicRedFlagReconcilerAdapter implements RedFlagReconciler {

    private static final String RED_FLAGS_YAML_PATH = "interview-rubric/red-flags.yaml";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 AI 면접 코치를 위해 지원자 답변에서 레드플래그(채점 신뢰도를 훼손하는 신호)를
            최종 확정하는 역할입니다. 라이브 면접 중 잠정으로만 기록된 신호와 포트폴리오 대조
            후보를 검토해, 실제로 레드플래그로 확정할지 결정합니다.

            아래 레드플래그 카탈로그(유형·탐지법·기본 영향 축·cap·knockout·노출 여부)를 따릅니다.
            %s

            입력으로 세 가지를 받습니다:
            1. [포트폴리오 대조 후보] - 포트폴리오에서 뽑은 캐물 지점(무엇을 캐물으려 했는지)과
               그 지점을 실제로 캐물은 턴 번호(usedInTurn). 실제로 질문된 후보만 제공됩니다.
               usedInTurn 턴의 답변 원문을 [턴 원문]에서 확인해, 포트폴리오 내용과 실제 답변이
               어긋나면 FABRICATION(지어냄) 후보입니다.
            2. [모순 후보] - 라이브 중 뒤 턴이 앞 턴의 전제를 뒤집어 잠정 기록된 지점. 원래
               발언 턴과 뒤집은 발언 턴 번호가 있습니다. CONTRADICTION(일관성 붕괴) 또는
               맥락에 따라 FABRICATION 후보입니다.
            3. [턴 원문] - 세션 전체 턴의 질문·답변 원문과 axis, 발화 구간.

            규칙:
            - 후보는 요약(echo/probe 인용문)만 보고 확정하지 말고, 반드시 [턴 원문]에서 해당
              턴의 실제 질문·답변 전체 맥락을 확인한 뒤에만 판단하세요.
            - FABRICATION·knockout은 특히 신중해야 합니다 - 관련 턴 원문을 다시 확인해 실제로
              앞뒤가 안 맞는지, 지원자가 스스로 정정했는지(정정이면 레드플래그 아님) 확인한
              뒤에만 확정하세요. STT 인식 오류로 인한 오탐 가능성도 고려하세요.
            - PERFECT_NARRATIVE·BLAME_SHIFTING·BUZZWORD_SALAD는 잠정 후보가 따로 없으므로,
              [턴 원문]에서 의심되는 축(특히 ④ 대안·우선순위, ⑤ 갈등)의 턴을 직접 검토해
              판단하세요.
            - 확정할 근거가 부족하면 레드플래그를 만들지 마세요. 의심만으로 확정하지 않습니다.
            - evidenceTimestamps는 판단 근거가 된 턴의 시작/종료 초를 인용하세요(모순은 원래
              발언과 뒤집은 발언 두 구간 모두 인용).
            - affectedTestType은 red-flags.yaml의 default_affected_axes를 기본으로 삼되,
              실제로 영향을 준 축이 다르면 관찰한 축으로 조정하세요. 특정 축이 아니라 전체
              신뢰도에 영향을 주는 경우(예: CONTRADICTION) null로 두세요.

            모든 검증을 마쳤다면, 최종 확정된 레드플래그만 담아 다른 설명 없이 JSON 배열
            하나만 반환하세요. 확정된 레드플래그가 없으면 빈 배열을 반환하세요. 각 원소는
            다음 필드를 가집니다:
            type(FABRICATION/CONTRADICTION/PERFECT_NARRATIVE/BLAME_SHIFTING/BUZZWORD_SALAD 중 하나),
            affectedTestType(depth/boundary/connection/tradeoff/conflict/resilience 또는 null),
            capValue(정수 또는 null), knockout(true/false),
            evidenceTimestamps(startSec/endSec 쌍의 배열, 비어 있을 수 있음), rationale.
            """;

    private final ChatClient chatClient;
    private final String systemPrompt;

    AnthropicRedFlagReconcilerAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(loadRedFlagsYaml());
    }

    @Override
    public List<RedFlagVerdict> reconcile(RedFlagReconcileContext context) {
        String userMessage = buildUserMessage(context);

        try {
            List<RedFlagVerdictLlmEntry> entries = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .entity(new ParameterizedTypeReference<List<RedFlagVerdictLlmEntry>>() {
                    });
            return entries.stream().map(this::toVerdict).toList();
        } catch (Exception e) {
            log.error("[RED FLAG RECONCILE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("레드플래그 확정에 실패했어요.", e);
        }
    }

    private String buildUserMessage(RedFlagReconcileContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("[포트폴리오 대조 후보]\n");
        if (context.portfolioCandidates().isEmpty()) {
            sb.append("(없음)\n");
        }
        for (PortfolioCandidate candidate : context.portfolioCandidates()) {
            sb.append("- axis: ").append(candidate.testType().name().toLowerCase())
                    .append(", usedInTurn: ").append(candidate.usedInTurn())
                    .append(", probe: ").append(candidate.probeText())
                    .append(", echo: ").append(candidate.echoQuote())
                    .append("\n");
        }

        sb.append("\n[모순 후보]\n");
        if (context.contradictionCandidates().isEmpty()) {
            sb.append("(없음)\n");
        }
        for (ContradictionCandidate candidate : context.contradictionCandidates()) {
            sb.append("- axis: ").append(candidate.testType().name().toLowerCase())
                    .append(", echo: ").append(candidate.echoQuote())
                    .append(", probe: ").append(candidate.probeText())
                    .append(", originTurn: ").append(candidate.originTurnNumber())
                    .append(", contradictingTurn: ").append(candidate.contradictingTurnNumber())
                    .append("\n");
        }

        sb.append("\n[턴 원문]\n");
        for (Turn turn : context.turns()) {
            sb.append(turn.turnNumber()).append(". axis=").append(turn.testType().name().toLowerCase()).append("\n");
            sb.append("   Q: ").append(turn.questionContent()).append("\n");
            sb.append("   A: ").append(turn.skipped() ? "(스킵됨)" : turn.answerText()).append("\n");
            sb.append("   (start=").append(turn.answerStartSec()).append(", end=").append(turn.answerEndSec()).append(")\n");
        }

        return sb.toString();
    }

    private RedFlagVerdict toVerdict(RedFlagVerdictLlmEntry entry) {
        List<TimeRange> evidenceTimestamps = entry.evidenceTimestamps() == null
                ? List.of()
                : entry.evidenceTimestamps().stream()
                        .map(t -> new TimeRange(t.startSec(), t.endSec()))
                        .toList();

        return new RedFlagVerdict(
                RedFlagType.valueOf(entry.type().toUpperCase()),
                StringUtils.hasText(entry.affectedTestType()) ? TestType.valueOf(entry.affectedTestType().toUpperCase()) : null,
                entry.capValue(),
                entry.knockout(),
                evidenceTimestamps,
                entry.rationale()
        );
    }

    private static String loadRedFlagsYaml() {
        try {
            return StreamUtils.copyToString(new ClassPathResource(RED_FLAGS_YAML_PATH).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("red-flags.yaml 로드에 실패했어요.", e);
        }
    }
}
