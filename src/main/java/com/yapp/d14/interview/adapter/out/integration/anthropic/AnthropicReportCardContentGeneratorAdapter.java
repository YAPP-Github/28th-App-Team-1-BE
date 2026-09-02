package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.common.metrics.AiCallMetrics;
import com.yapp.d14.common.metrics.AiCallStage;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext.AxisCardInput;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext.Turn;
import com.yapp.d14.interview.application.port.out.ReportCardContentGenerator;
import com.yapp.d14.interview.application.port.out.ReportCardDraft;
import com.yapp.d14.interview.domain.HighlightReason;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TextRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
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

            1. questionIntentTitle(질문 의도 제목) + questionIntentTranslation(질문 분석) - 이 턴의
               질문에서 무엇을 확인하려 했는지를 지원자가 이해할 수 있는 말로 설명합니다. 내부
               채점 용어(축·천장·resolution 등)는 쓰지 않습니다. 같은 axis의 다른 턴과 내용이
               겹치더라도, 그 턴 자체의 질문 의도를 기준으로 각자 다시 씁니다.
               questionIntentTitle은 이 질문의 핵심 주제를 짧은 명사구로 요약합니다(대략
               10~15자 내외, 문장부호 없이. 예: "트래픽 확장 대응 전략"). questionIntentTranslation은
               그 제목을 풀어 이 질문이 실제로 무엇을, 왜 확인하려 하는지 1~2문장으로 설명합니다
               (예: "트래픽이 증가했을 때 발생할 병목지점과 시스템의 한계, 그리고 이를 어떻게
               판단할지 설명하는 질문입니다").

            2. highlightSpans(대본 하이라이트) - 그 턴의 답변(answerText) 중 채점 근거가 된
               구간마다 하나씩 만듭니다. quote는 그 구간을 answerText에서 한 글자도 틀리지
               않게 그대로 옮겨 적은 원문입니다(요약·의역 금지, 공백·문장부호까지 answerText와
               정확히 일치해야 서버가 위치를 찾을 수 있습니다). highlightSpans는 answerText에
               등장하는 순서대로(앞에서 뒤로) 반환합니다. 다른 하이라이트 구간과 겹치지
               않게 합니다. tone은 GOOD(잘함) 또는 IMPROVE(개선)입니다. analysis(답변 분석)는 그 구간이
               왜 GOOD인지 또는 왜 IMPROVE인지를 1~2문장으로 설명합니다 — 근거가 된 사실을
               짚고, IMPROVE라면 무엇이 부족한지를, GOOD이라면 무엇이 효과적인지를 씁니다.
               답변에 없는 것은 추측해서 쓰지 않습니다(자신감·긴장·표정·목소리 톤·성격·감정
               같은 인상 표현 금지, 관찰된 사실만 근거로 씁니다).

               title(제목)은 이 하이라이트를 한 줄로 요약한 짧은 명사구입니다(대략 12자 내외,
               문장부호 없이). GOOD이면 잘한 점을(예: "명확한 원인과 구조 설명"), IMPROVE이면
               핵심 문제나 개선점을(예: "질문 의도와 다르게 답변", "구체적인 사례 제시") 명명합니다.

               reason(개선유형)은 이 구간이 질문에 대해 어떤 상태인지를 하나로 판정한 값입니다.
               먼저 tone을 정한 뒤 아래 순서로 고릅니다.
               - tone=IMPROVE인 경우:
                 1) 이 구간이 질문이 실제로 요구한 것과 다른 주제/딴 답이면 → OFF_INTENT
                 2) 그렇지 않고 너무 짧거나 막연해 무슨 경험·근거인지 드러나지 않아 캐물 구체적
                    실마리조차 없으면 → SHALLOW
                 3) 방향은 맞고 내용도 있으나 빠진 근거·수치·대안이 있어 캐물어 보완을 유도할 수
                    있으면 → PROBE_WORTHY
               - tone=GOOD인 경우:
                 1) 원인·한계·결과까지 스스로 짚어 더 물을 자연스러운 지점이 남지 않았으면 → SUFFICIENT
                 2) 강점의 진위·경계·트레이드오프를 더 시험할 여지가 있으면 → PROBE_WORTHY

               followUpQuestions(추가 질문)는 reason=PROBE_WORTHY일 때만 1~3개 만듭니다. 나머지
               reason(OFF_INTENT·SHALLOW·SUFFICIENT)에서는 반드시 빈 배열로 둡니다. 이는 그
               구간(하이라이트가 잡은 답변 부분)을 두고 면접관이 실제로 이어서 던질 법한
               꼬리질문입니다. 아래 [꼬리질문 생성 원칙]을 전술로 삼되, 원칙 번호나 내부 용어는
               질문에 노출하지 말고, 그 구간의 실제 내용에 밀착한 구체적 질문을 만듭니다(일반론
               금지). tone=GOOD이면 더 깊이 파고들어 진위·한계를 시험하는 질문을, tone=IMPROVE이면
               부족한 부분을 드러내거나 해명을 요구하는 질문을 위주로 만듭니다. 각 질문은 실제
               면접관이 말하듯 한 문장으로 씁니다.

               answerTopicTitle(내 답변 요지)은 reason=OFF_INTENT일 때만 작성합니다. 질문이 요구한
               것과 별개로, 지원자의 답변이 실제로 다룬 주제가 무엇인지를 짧은 명사구로 요약합니다
               (예: 질문은 "장애 원인을 좁혀가는 순서"인데 답변이 팀 관계 이야기였다면 "팀 내 신뢰와 성향").
               "질문 의도(questionIntentTitle) ↔ 내 답변(answerTopicTitle)" 대비로 보여줄 값이니,
               비난·평가 없이 답변의 실제 주제만 담습니다. reason이 OFF_INTENT가 아니면 빈 문자열로 둡니다.

            resolutionLevel=LOW인 axis에 속한 턴(카드) 전부에 적용되는 처리:
            - resolutionLowReason=FEW_TURNS 또는 SHALLOW_ANSWER(짧음·얕음): 능력을 판단하는
              분석은 보류합니다. highlightSpans는 빈 배열로 두고, questionIntentTitle·
              questionIntentTranslation만 작성합니다.
            - resolutionLowReason=OFF_TOPIC(딴 답): questionIntentTitle·questionIntentTranslation은
              작성하고, 질문과 무관하게 답한 구간 하나를 tone=IMPROVE·reason=OFF_INTENT
              하이라이트로 잡습니다.

            출력은 다른 설명 없이 JSON 배열 하나만 반환하세요. 배열의 원소 개수는 입력으로 받은
            턴의 총 개수와 정확히 같아야 하며, 각 원소는 다음 필드를 가집니다:
            questionId(입력에서 받은 값을 그대로 echo — 어느 턴의 카드인지 식별하는 데만 쓰이니
            입력에 없던 값을 지어내지 마세요),
            questionIntentTitle(문자열), questionIntentTranslation(문자열),
            highlightSpans(quote(answerText 원문 그대로, answerText에 등장하는 순서대로)/
            tone(GOOD 또는 IMPROVE)/
            reason(PROBE_WORTHY/OFF_INTENT/SHALLOW/SUFFICIENT)/title(문자열)/analysis(문자열)/
            followUpQuestions(문자열 배열, PROBE_WORTHY일 때만 1~3개, 그 외엔 빈 배열)/
            answerTopicTitle(문자열, OFF_INTENT일 때만 내 답변 요지 명사구, 그 외엔 빈 문자열)의 배열,
            비어 있을 수 있음).

            [꼬리질문 생성 원칙]
            %s
            """;

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final AnthropicUsageRecorder anthropicUsageRecorder;
    private final AiCallMetrics aiCallMetrics;

    AnthropicReportCardContentGeneratorAdapter(
            @Qualifier("anthropicChatModel") ChatModel chatModel,
            AnthropicUsageRecorder anthropicUsageRecorder,
            AiCallMetrics aiCallMetrics
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(loadPrinciplesYaml());
        this.anthropicUsageRecorder = anthropicUsageRecorder;
        this.aiCallMetrics = aiCallMetrics;
    }

    @Override
    public List<ReportCardDraft> generate(Long sessionId, ReportCardContentContext context) {
        String userMessage = buildUserMessage(context);
        Map<Long, TurnRef> turnRefsById = indexTurnsByQuestionId(context);

        try {
            return aiCallMetrics.record(AiCallStage.REPORT_CARD, () -> {
                ResponseEntity<ChatResponse, List<ReportCardContentLlmEntry>> responseEntity = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .call()
                        .responseEntity(new ParameterizedTypeReference<List<ReportCardContentLlmEntry>>() {
                        });
                anthropicUsageRecorder.record(sessionId, responseEntity.response());
                return toDrafts(responseEntity.entity(), turnRefsById);
            });
        } catch (Exception e) {
            log.error("[REPORT CARD CONTENT GENERATE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("리포트 카드 생성에 실패했어요.", e);
        }
    }

    // questionId → 그 턴의 서버 확정값(testType·depthLevel·answerText). LLM echo가 아니라 이 값을 카드에 쓰고,
    // answerText는 highlightSpans의 quote를 찾을 원문으로 쓴다.
    private Map<Long, TurnRef> indexTurnsByQuestionId(ReportCardContentContext context) {
        Map<Long, TurnRef> turnRefsById = new HashMap<>();
        for (AxisCardInput card : context.axisCards()) {
            for (Turn turn : card.turns()) {
                turnRefsById.put(turn.questionId(), new TurnRef(card.testType(), turn.depthLevel(), turn.answerText()));
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

    private record TurnRef(TestType testType, int depthLevel, String answerText) {
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
        List<HighlightSpan> highlightSpans = toHighlightSpans(entry.highlightSpans(), turnRef.answerText());

        return new ReportCardDraft(
                entry.questionId(),
                turnRef.depthLevel(),
                turnRef.testType(),
                entry.questionIntentTitle(),
                entry.questionIntentTranslation(),
                highlightSpans
        );
    }

    // package-private static: LLM 호출 없이 quote 탐색(순서 기반 커서)·reason 폴백·꼬리질문 게이팅 규칙을
    // 단위 테스트로 고정하기 위함. LLM이 startIndex/endIndex를 직접 세지 않고 quote(원문 그대로)만 반환하므로,
    // answerText에서 quote를 찾아 실제 위치를 되찾는다. 커서 이후로 먼저 찾아 반복되는 문구를 등장 순서대로
    // 소비하고, 못 찾으면 전체에서 재검색(순서 지시 위반 대비)하며, 그래도 못 찾으면(할루시네이션) 그 하이라이트를 버린다.
    static List<HighlightSpan> toHighlightSpans(
            List<ReportCardContentLlmEntry.HighlightSpanLlmEntry> entries, String answerText
    ) {
        if (entries == null || entries.isEmpty() || answerText == null || answerText.isEmpty()) {
            return List.of();
        }
        List<HighlightSpan> spans = new ArrayList<>();
        int cursor = 0;
        for (ReportCardContentLlmEntry.HighlightSpanLlmEntry entry : entries) {
            HighlightSpan span = toHighlightSpan(entry, answerText, cursor);
            if (span == null) {
                continue; // 실패 사유(quote 미검출/tone 파싱 실패)는 toHighlightSpan에서 개별 로깅한다.
            }
            spans.add(span);
            cursor = span.range().endIndex();
        }
        return spans;
    }

    private static HighlightSpan toHighlightSpan(
            ReportCardContentLlmEntry.HighlightSpanLlmEntry entry, String answerText, int cursor
    ) {
        String piece = entry.quote() == null ? "" : entry.quote().strip();
        if (piece.isEmpty()) {
            return null;
        }
        int start = locate(answerText, piece, cursor);
        if (start < 0) {
            log.warn("[REPORT CARD HIGHLIGHT] quote를 answerText에서 찾지 못해 하이라이트 제외: quote={}", entry.quote());
            return null;
        }
        int end = start + piece.length();

        HighlightTone tone = resolveTone(entry.tone());
        if (tone == null) {
            log.warn("[REPORT CARD HIGHLIGHT] tone을 해석할 수 없어 하이라이트 제외: tone={}", entry.tone());
            return null;
        }
        List<String> followUpQuestions = entry.followUpQuestions() == null
                ? List.of()
                : entry.followUpQuestions();
        HighlightReason reason = resolveReason(entry.reason(), tone, followUpQuestions, entry.answerTopicTitle());
        // A/B/C를 reason 단일 소스로 결정론적으로 만든다: PROBE_WORTHY가 아니면 꼬리질문은 무조건 비운다(LLM 누출 방지).
        List<String> gatedFollowUps = reason == HighlightReason.PROBE_WORTHY ? followUpQuestions : List.of();
        // answerTopic(내 답변 요지)는 OFF_INTENT 대비 UI 전용이므로 그 외 reason에서는 LLM이 넣었어도 버린다.
        String answerTopicTitle = reason == HighlightReason.OFF_INTENT ? trimToNull(entry.answerTopicTitle()) : null;
        return new HighlightSpan(
                new TextRange(start, end),
                tone,
                reason,
                entry.title(),
                entry.analysis(),
                gatedFollowUps,
                answerTopicTitle
        );
    }

    // LLM이 tone을 누락하거나 오타를 냈을 때 예외 대신 null을 돌려주는 방어적 파싱.
    // 하이라이트 하나의 형식 오류 때문에 리포트 카드 생성 전체(재시도 소진 후 FAILED)가 실패하지 않도록 한다.
    private static HighlightTone resolveTone(String rawTone) {
        if (rawTone == null) {
            return null;
        }
        try {
            return HighlightTone.valueOf(rawTone.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // 커서 이후로 우선 검색해 반복되는 문구를 답변에 등장하는 순서대로 소비한다. LLM이 지시한 하이라이트 순서가
    // 실제 답변 내 등장 순서와 다를 수 있어(ReportCardHighlightMappingTest의 "순서 지시를 어겨도..." 케이스),
    // ScriptSegmentMapper(STT 세그먼트는 항상 시간 순)와 달리 여기서는 cursor 이전으로의 폴백도 그대로 허용한다.
    private static int locate(String answerText, String piece, int cursor) {
        int found = answerText.indexOf(piece, cursor);
        return found >= 0 ? found : answerText.indexOf(piece);
    }

    // LLM이 reason을 누락·오타 냈을 때의 방어적 폴백. 꼬리질문·answerTopicTitle 유무로 톤과 함께 가장 그럴듯한 값을 고른다.
    private static HighlightReason resolveReason(
            String rawReason, HighlightTone tone, List<String> followUpQuestions, String rawAnswerTopicTitle
    ) {
        if (rawReason != null) {
            try {
                return HighlightReason.valueOf(rawReason.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 값이면 아래 폴백으로 넘어간다.
            }
        }
        if (!followUpQuestions.isEmpty()) {
            return HighlightReason.PROBE_WORTHY;
        }
        // answerTopicTitle은 프롬프트상 OFF_INTENT일 때만 LLM이 채우므로, reason이 없어도 이 값이 있으면
        // SHALLOW보다 OFF_INTENT일 가능성이 훨씬 높다(SHALLOW·SUFFICIENT는 이 값을 채울 근거가 없음).
        if (tone == HighlightTone.IMPROVE && trimToNull(rawAnswerTopicTitle) != null) {
            return HighlightReason.OFF_INTENT;
        }
        return tone == HighlightTone.GOOD ? HighlightReason.SUFFICIENT : HighlightReason.SHALLOW;
    }

    private static String loadPrinciplesYaml() {
        try {
            return StreamUtils.copyToString(new ClassPathResource(PRINCIPLES_YAML_PATH).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("principles.yaml 로드에 실패했어요.", e);
        }
    }
}
