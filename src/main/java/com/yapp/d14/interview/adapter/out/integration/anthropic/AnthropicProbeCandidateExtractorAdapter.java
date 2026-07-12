package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.ProbeCandidateExtractor;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
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
class AnthropicProbeCandidateExtractorAdapter implements ProbeCandidateExtractor {

    private static final String AXES_YAML_PATH = "interview-rubric/axes.yaml";

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 AI 면접관을 위해 "캐물지점"(면접에서 더 파고들어 물어볼 만한 지점) 후보를 뽑는 역할입니다.
            입력으로 지원자 포트폴리오 청크와, 참고용 JD 키워드(있을 수도 없을 수도 있음)를 받습니다.

            아래 6대 평가 항목(axis) 정의를 기준으로 각 캐물지점에 axis 태그를 답니다.
            %s

            규칙:
            - 캐물지점은 반드시 포트폴리오 내용에 근거해야 합니다. JD 키워드만으로 캐물지점을 만들지 마세요.
            - jdKeywords와 겹치는 캐물지점이 있으면 jdMatch 필드에 그 키워드를 그대로 적으세요. 안 겹치면 null.
            - probeText는 "무엇을 캐물을지"에 대한 내부 메모(질문 문장 아님), echoQuote는 질문할 때 그대로 되받아 물을 원 표현입니다.
            - strength는 신호가 진할수록 high, 약하면 low로 답니다.
            - 개수를 인위적으로 채우거나 줄이지 마세요. 포트폴리오 분량에서 자연스럽게 나오는 만큼만 뽑습니다.

            출력은 다른 설명 없이 JSON 배열 하나만 반환하세요. 각 원소는 다음 필드를 가집니다:
            axis(depth/boundary/connection/tradeoff/conflict/resilience 중 하나),
            secondaryAxis(같은 값 또는 null), probeText, echoQuote, jdMatch(문자열 또는 null),
            strength(high/mid/low 중 하나).
            """;

    private final ChatClient chatClient;
    private final String systemPrompt;

    AnthropicProbeCandidateExtractorAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(loadAxesYaml());
    }

    @Override
    public List<ProbeCandidateDraft> extract(List<String> portfolioChunks, List<String> jdKeywords) {
        String userMessage = buildUserMessage(portfolioChunks, jdKeywords);

        try {
            List<ProbeCandidateLlmEntry> entries = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .entity(new ParameterizedTypeReference<List<ProbeCandidateLlmEntry>>() {
                    });
            return entries.stream().map(this::toDraft).toList();
        } catch (Exception e) {
            log.error("[PROBE CANDIDATE EXTRACT] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("캐물지점 추출에 실패했어요.", e);
        }
    }

    private String buildUserMessage(List<String> portfolioChunks, List<String> jdKeywords) {
        StringBuilder sb = new StringBuilder();
        sb.append("[포트폴리오 청크]\n");
        for (String chunk : portfolioChunks) {
            sb.append("- ").append(chunk).append("\n");
        }
        if (jdKeywords != null && !jdKeywords.isEmpty()) {
            sb.append("\n[JD 키워드 (참고용)]\n").append(String.join(", ", jdKeywords)).append("\n");
        }
        return sb.toString();
    }

    private ProbeCandidateDraft toDraft(ProbeCandidateLlmEntry entry) {
        return new ProbeCandidateDraft(
                TestType.valueOf(entry.axis().toUpperCase()),
                StringUtils.hasText(entry.secondaryAxis()) ? TestType.valueOf(entry.secondaryAxis().toUpperCase()) : null,
                entry.probeText(),
                entry.echoQuote(),
                entry.jdMatch(),
                QuestionCandidateStrength.valueOf(entry.strength().toUpperCase())
        );
    }

    private static String loadAxesYaml() {
        try {
            return StreamUtils.copyToString(new ClassPathResource(AXES_YAML_PATH).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("axes.yaml 로드에 실패했어요.", e);
        }
    }
}
