package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.ReportCardContentContext;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext.AxisCardInput;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext.Turn;
import com.yapp.d14.interview.application.port.out.ReportCardContentGenerator;
import com.yapp.d14.interview.application.port.out.ReportCardDraft;
import com.yapp.d14.interview.domain.ActionKeyword;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.RewriteSuggestion;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TimeRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
class AnthropicReportCardContentGeneratorAdapter implements ReportCardContentGenerator {

    private static final int MAX_ACTION_KEYWORDS = 3;

    private static final String SYSTEM_PROMPT = """
            당신은 AI 면접 코치를 위해 리포트의 카드 내용을 작성하는 역할입니다. 카드는
            질문/답변 턴 하나당 하나입니다(축 전체를 묶은 카드가 아닙니다). 입력으로 axis별
            질문-답변 턴 목록과, 그 axis 전체에 적용되는 채점 근거(rationale)·해상도
            (resolutionLevel)를 받습니다. 같은 axis에 턴이 여러 개면, 그 턴들끼리는 서로
            문맥(같은 채점 근거)을 공유하되, 산출물(질문 분석·하이라이트·키워드·고쳐쓰기)은
            턴마다 독립적으로 작성합니다.

            턴(카드)마다 아래 4가지를 만듭니다.

            1. questionIntentTranslation(질문 분석) - 이 턴의 질문에서 무엇을 확인하려 했는지를
               지원자가 이해할 수 있는 말로 풀어씁니다. 내부 채점 용어(축·천장·resolution 등)는
               쓰지 않습니다. 같은 axis의 다른 턴과 내용이 겹치더라도, 그 턴 자체의 질문 의도를
               기준으로 각자 다시 씁니다.

            2. highlightSpans(대본 하이라이트) - 그 턴의 답변 중 채점 근거가 된 구간에
               잘함(GOOD)/개선(IMPROVE) 구분을 붙입니다. 시작·종료 초는 반드시 그 턴의
               answerStartSec~answerEndSec 범위 안에서 고릅니다(다른 턴의 구간을 침범하지
               않습니다).

            3. actionKeywords(행동형 키워드, 턴당 최대 3개) - 문제를 지적하는 말이 아니라
               다음 면접에서 바로 실천할 행동으로 씁니다.
               - 좋은 예: "구체적인 사례 제시", "결론 먼저 말하기", "성과를 수치로 설명하기"
               - 금지 예: "근거 부족", "자신감 부족", "답변이 길다", "전달력이 부족하다" 등
                 주관적이거나 음성만으로 판단하기 모호한 표현
               - 우선순위(개선점이 여러 개면 이 순서로 최대 3개만 고른다):
                 1) 질문 적합성(질문 의도와 다른 방향 답변) 2) 전달력 핵심(결론 먼저 말하기 등)
                 3) 직군 핵심 역량 개선 4) 표현·구조 개선
               - 같은 행동으로 해결되는 개선점은 하나로 묶습니다.
               - problemAnalysis(문제 분석)는 약 30%, improvementReason+applicationMethod
                 (개선 이유+적용 방법)는 약 70% 비중으로 씁니다.
               - 답변에 없는 것은 추측해서 쓰지 않습니다(자신감·긴장·표정·목소리 톤·성격·감정 금지).

            4. rewriteSuggestion(이렇게 바꿔 말해보세요) - 그 턴에서 사용자가 실제로 말한 문장만
               재료로 더 나은 표현으로 고쳐 씁니다. 사용자가 말하지 않은 경험·사실·수치를 새로
               만들어 넣지 않습니다. 원 답변이 너무 빈약해 고쳐 쓸 재료가 없으면 null로 생략합니다.

            resolutionLevel=LOW인 axis에 속한 턴(카드) 전부에 적용되는 처리:
            - resolutionLowReason=FEW_TURNS 또는 SHALLOW_ANSWER(짧음·얕음): 능력을 판단하는
              분석은 보류합니다. actionKeywords와 rewriteSuggestion은 빈 배열/null로 두고,
              questionIntentTranslation만 작성합니다.
            - resolutionLowReason=OFF_TOPIC(딴 답): questionIntentTranslation은 작성하고,
              actionKeywords에 질문 적합성 키워드("질문이 묻는 것부터 답하기" 류) 하나는
              제공할 수 있습니다. rewriteSuggestion은 답변이 질문과 무관하므로 생략합니다.

            출력은 다른 설명 없이 JSON 배열 하나만 반환하세요. 배열의 원소 개수는 입력으로 받은
            턴의 총 개수와 정확히 같아야 하며, 각 원소는 다음 필드를 가집니다:
            axis(depth/boundary/connection/tradeoff/conflict/resilience 중 하나),
            questionId(입력에서 받은 값을 그대로 echo), depthLevel(입력에서 받은 값을 그대로 echo),
            questionIntentTranslation(문자열),
            highlightSpans(startSec/endSec/tone(GOOD 또는 IMPROVE)의 배열, 비어 있을 수 있음),
            actionKeywords(keyword/problemAnalysis/improvementReason/applicationMethod/priority의
            배열, 최대 3개, 비어 있을 수 있음),
            rewriteSuggestion(originalQuote/rewrittenText 객체 또는 null).
            """;

    private final ChatClient chatClient;

    AnthropicReportCardContentGeneratorAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public List<ReportCardDraft> generate(ReportCardContentContext context) {
        String userMessage = buildUserMessage(context);

        try {
            List<ReportCardContentLlmEntry> entries = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .entity(new ParameterizedTypeReference<List<ReportCardContentLlmEntry>>() {
                    });
            return entries.stream().map(this::toDraft).toList();
        } catch (Exception e) {
            log.error("[REPORT CARD CONTENT GENERATE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("리포트 카드 생성에 실패했어요.", e);
        }
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
                sb.append("   (answerStartSec=").append(turn.answerStartSec())
                        .append(", answerEndSec=").append(turn.answerEndSec()).append(")\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private ReportCardDraft toDraft(ReportCardContentLlmEntry entry) {
        List<HighlightSpan> highlightSpans = entry.highlightSpans() == null
                ? List.of()
                : entry.highlightSpans().stream()
                        .map(h -> new HighlightSpan(
                                new TimeRange(h.startSec(), h.endSec()),
                                HighlightTone.valueOf(h.tone().toUpperCase())
                        ))
                        .toList();

        List<ActionKeyword> actionKeywords = entry.actionKeywords() == null
                ? List.of()
                : entry.actionKeywords().stream()
                        .limit(MAX_ACTION_KEYWORDS)
                        .map(k -> new ActionKeyword(
                                k.keyword(),
                                k.problemAnalysis(),
                                k.improvementReason(),
                                k.applicationMethod(),
                                k.priority()
                        ))
                        .toList();

        RewriteSuggestion rewriteSuggestion = entry.rewriteSuggestion() == null
                ? null
                : new RewriteSuggestion(entry.rewriteSuggestion().originalQuote(), entry.rewriteSuggestion().rewrittenText());

        return new ReportCardDraft(
                entry.questionId(),
                entry.depthLevel(),
                TestType.valueOf(entry.axis().toUpperCase()),
                entry.questionIntentTranslation(),
                highlightSpans,
                actionKeywords,
                rewriteSuggestion
        );
    }
}
