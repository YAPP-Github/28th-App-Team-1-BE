package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.ReportCardContentContext;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext.AxisCardInput;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext.Turn;
import com.yapp.d14.interview.application.port.out.ReportCardContentGenerator;
import com.yapp.d14.interview.application.port.out.ReportCardDraft;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TextRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
class AnthropicReportCardContentGeneratorAdapter implements ReportCardContentGenerator {

    // 꼬리질문(followUpQuestions) 생성 전술의 근거로 재사용한다.
    private static final String PRINCIPLES_YAML_PATH = "interview-rubric/principles.yaml";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 AI 면접 코치를 위해 리포트의 카드 내용을 작성하는 역할입니다. 카드는
            질문/답변 턴 하나당 하나입니다(축 전체를 묶은 카드가 아닙니다). 입력으로 axis별
            질문-답변 턴 목록과, 그 axis 전체에 적용되는 채점 근거(rationale)·해상도
            (resolutionLevel)를 받습니다. 같은 axis에 턴이 여러 개면, 그 턴들끼리는 서로
            문맥(같은 채점 근거)을 공유하되, 산출물(질문 분석·하이라이트)은 턴마다
            독립적으로 작성합니다.

            턴(카드)마다 아래를 만듭니다.

            1. questionIntentTranslation(질문 분석) - 이 턴의 질문에서 무엇을 확인하려 했는지를
               지원자가 이해할 수 있는 말로 풀어씁니다. 내부 채점 용어(축·천장·resolution 등)는
               쓰지 않습니다. 같은 axis의 다른 턴과 내용이 겹치더라도, 그 턴 자체의 질문 의도를
               기준으로 각자 다시 씁니다.

            2. highlightSpans(대본 하이라이트) - 그 턴의 답변(answerText) 중 채점 근거가 된
               구간마다 하나씩 만듭니다. startIndex/endIndex는 answerText 문자열의 0부터
               시작하는 문자 인덱스입니다(startIndex 포함, endIndex 미포함). 반드시 그 턴의
               answerText 길이 범위 안에서 고르고, 다른 하이라이트 구간과 겹치지 않게 합니다.
               tone은 GOOD(잘함) 또는 IMPROVE(개선)입니다. analysis(답변 분석)는 그 구간이
               왜 GOOD인지 또는 왜 IMPROVE인지를 1~2문장으로 설명합니다 — 근거가 된 사실을
               짚고, IMPROVE라면 무엇이 부족한지를, GOOD이라면 무엇이 효과적인지를 씁니다.
               답변에 없는 것은 추측해서 쓰지 않습니다(자신감·긴장·표정·목소리 톤·성격·감정
               같은 인상 표현 금지, 관찰된 사실만 근거로 씁니다).

               각 하이라이트에는 followUpQuestions(추가 질문)도 0~3개 만듭니다. 이는 그
               구간(하이라이트가 잡은 답변 부분)을 두고 면접관이 실제로 이어서 던질 법한
               꼬리질문입니다. 아래 [꼬리질문 생성 원칙]을 전술로 삼되, 원칙 번호나 내부 용어는
               질문에 노출하지 말고, 그 구간의 실제 내용에 밀착한 구체적 질문을 만듭니다(일반론
               금지). tone=GOOD이면 더 깊이 파고들어 진위·한계를 시험하는 질문을, tone=IMPROVE이면
               부족한 부분을 드러내거나 해명을 요구하는 질문을 위주로 만듭니다. 마땅한 질문거리가
               없으면 빈 배열로 둡니다. 각 질문은 실제 면접관이 말하듯 한 문장으로 씁니다.

            resolutionLevel=LOW인 axis에 속한 턴(카드) 전부에 적용되는 처리:
            - resolutionLowReason=FEW_TURNS 또는 SHALLOW_ANSWER(짧음·얕음): 능력을 판단하는
              분석은 보류합니다. highlightSpans는 빈 배열로 두고, questionIntentTranslation만
              작성합니다.
            - resolutionLowReason=OFF_TOPIC(딴 답): questionIntentTranslation은 작성하고,
              질문과 무관하게 답한 구간 하나를 tone=IMPROVE 하이라이트로 잡습니다.

            출력은 다른 설명 없이 JSON 배열 하나만 반환하세요. 배열의 원소 개수는 입력으로 받은
            턴의 총 개수와 정확히 같아야 하며, 각 원소는 다음 필드를 가집니다:
            questionId(입력에서 받은 값을 그대로 echo — 어느 턴의 카드인지 식별하는 데만 쓰이니
            입력에 없던 값을 지어내지 마세요),
            questionIntentTranslation(문자열),
            highlightSpans(startIndex/endIndex/tone(GOOD 또는 IMPROVE)/analysis(문자열)/
            followUpQuestions(문자열 배열, 0~3개, 비어 있을 수 있음)의 배열, 비어 있을 수 있음).

            [꼬리질문 생성 원칙]
            %s
            """;

    private final ChatClient chatClient;
    private final String systemPrompt;

    AnthropicReportCardContentGeneratorAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(loadPrinciplesYaml());
    }

    @Override
    public List<ReportCardDraft> generate(ReportCardContentContext context) {
        String userMessage = buildUserMessage(context);
        Map<Long, TurnRef> turnRefsById = indexTurnsByQuestionId(context);

        try {
            List<ReportCardContentLlmEntry> entries = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .entity(new ParameterizedTypeReference<List<ReportCardContentLlmEntry>>() {
                    });
            return toDrafts(entries, turnRefsById);
        } catch (Exception e) {
            log.error("[REPORT CARD CONTENT GENERATE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("리포트 카드 생성에 실패했어요.", e);
        }
    }

    // questionId → 그 턴의 서버 확정값(testType·depthLevel). LLM echo가 아니라 이 값을 카드에 쓴다.
    private Map<Long, TurnRef> indexTurnsByQuestionId(ReportCardContentContext context) {
        Map<Long, TurnRef> turnRefsById = new HashMap<>();
        for (AxisCardInput card : context.axisCards()) {
            for (Turn turn : card.turns()) {
                turnRefsById.put(turn.questionId(), new TurnRef(card.testType(), turn.depthLevel()));
            }
        }
        return turnRefsById;
    }

    // LLM이 questionId를 누락·환각하거나 같은 턴을 중복 반환하면 그 엔트리는 카드로 만들지 않는다
    // (없는 questionId로 카드를 만들면 저장 실패 또는 질문/대본이 빈 카드가 된다).
    private List<ReportCardDraft> toDrafts(List<ReportCardContentLlmEntry> entries, Map<Long, TurnRef> turnRefsById) {
        List<ReportCardDraft> drafts = new ArrayList<>();
        Set<Long> usedQuestionIds = new HashSet<>();
        for (ReportCardContentLlmEntry entry : entries) {
            Long questionId = entry.questionId();
            TurnRef turnRef = questionId == null ? null : turnRefsById.get(questionId);
            if (turnRef == null) {
                log.warn("[REPORT CARD CONTENT GENERATE] 입력에 없는 questionId 카드 무시: questionId={}", questionId);
                continue;
            }
            if (!usedQuestionIds.add(questionId)) {
                log.warn("[REPORT CARD CONTENT GENERATE] 중복 questionId 카드 무시: questionId={}", questionId);
                continue;
            }
            drafts.add(toDraft(entry, turnRef));
        }
        if (drafts.size() != turnRefsById.size()) {
            log.warn("[REPORT CARD CONTENT GENERATE] 카드 수 불일치: 턴={}, 생성={}", turnRefsById.size(), drafts.size());
        }
        return drafts;
    }

    private record TurnRef(TestType testType, int depthLevel) {
    }

    private String buildUserMessage(ReportCardContentContext context) {
        StringBuilder sb = new StringBuilder();
        for (AxisCardInput card : context.axisCards()) {
            sb.append("[axis: ").append(card.testType().name().toLowerCase()).append("]\n");
            sb.append("scoringRationale: ").append(card.scoringRationale()).append("\n");
            sb.append("resolutionLevel: ").append(card.resolutionLevel()).append("\n");
            if (card.resolutionLowReason() != null) {
                sb.append("resolutionLowReason: ").append(card.resolutionLowReason()).append("\n");
            }
            for (Turn turn : card.turns()) {
                sb.append("- questionId=").append(turn.questionId())
                        .append(", depthLevel=").append(turn.depthLevel()).append("\n");
                sb.append("   Q: ").append(turn.questionContent()).append("\n");
                sb.append("   A: ").append(turn.skipped() ? "(스킵됨)" : turn.answerText()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private ReportCardDraft toDraft(ReportCardContentLlmEntry entry, TurnRef turnRef) {
        List<HighlightSpan> highlightSpans = entry.highlightSpans() == null
                ? List.of()
                : entry.highlightSpans().stream()
                        .map(this::toHighlightSpan)
                        .toList();

        return new ReportCardDraft(
                entry.questionId(),
                turnRef.depthLevel(),
                turnRef.testType(),
                entry.questionIntentTranslation(),
                highlightSpans
        );
    }

    private HighlightSpan toHighlightSpan(ReportCardContentLlmEntry.HighlightSpanLlmEntry entry) {
        List<String> followUpQuestions = entry.followUpQuestions() == null
                ? List.of()
                : entry.followUpQuestions();
        return new HighlightSpan(
                new TextRange(entry.startIndex(), entry.endIndex()),
                HighlightTone.valueOf(entry.tone().toUpperCase()),
                entry.analysis(),
                followUpQuestions
        );
    }

    private static String loadPrinciplesYaml() {
        try {
            return StreamUtils.copyToString(new ClassPathResource(PRINCIPLES_YAML_PATH).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("principles.yaml 로드에 실패했어요.", e);
        }
    }
}
