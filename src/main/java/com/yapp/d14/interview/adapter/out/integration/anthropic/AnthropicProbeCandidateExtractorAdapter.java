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
    // InterviewSessionPreloadService.MAX_CANDIDATES 와 동일하게 맞춘다 — 여기서 이미 상위 N개로 걸러줘야
    // Java 쪽 List::limit이 임의 순서로 잘라내면서 관련도 낮은 후보가 살아남는 일을 막을 수 있다.
    private static final int MAX_CANDIDATES = 5;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 AI 면접관을 위해 "캐물지점"(면접에서 더 파고들어 물어볼 만한 지점) 후보를 뽑는 역할입니다.
            입력으로 지원자 포트폴리오 청크와, 참고용 JD 키워드(있을 수도 없을 수도 있음)를 받습니다.

            아래 6대 평가 항목(axis) 정의를 기준으로 각 캐물지점에 axis 태그를 답니다.
            %s

            규칙:
            - 캐물지점은 반드시 포트폴리오 내용에 근거해야 합니다. JD 키워드만으로 캐물지점을 만들지 마세요.
            - [집중 프로젝트]가 주어진 경우, 포트폴리오 청크는 검색으로 가져온 것이라 그와 무관한 다른 프로젝트/경험 청크가
              섞여 있을 수 있습니다. [집중 프로젝트]와 명확히 관련 없는 청크는 캐물지점의 근거로 쓰지 마세요.
            - jdKeywords와 겹치는 캐물지점이 있으면 jdMatch 필드에 그 키워드를 그대로 적으세요. 안 겹치면 null.
            - probeText는 "무엇을 캐물을지"에 대한 내부 메모(질문 문장 아님), echoQuote는 질문할 때 그대로 되받아 물을 원 표현입니다.
            - strength는 반드시 high/mid/low 중 하나로만 답합니다(medium 등 다른 표현 금지).
              신호가 진할수록 high, 애매하면 mid, 약하면 low로 답니다.
            - 최대 %d개까지만 반환하세요. [집중 프로젝트]와의 관련성과 신호 강도(strength)가 가장 강한 캐물지점 순으로
              고르세요. 포트폴리오 분량이 적어 그보다 적게 나올 수도 있지만, 개수를 채우려고 억지로 만들지는 마세요.

            출력은 다른 설명 없이 JSON 배열 하나만 반환하세요. 각 원소는 다음 필드를 가집니다:
            axis(depth/boundary/connection/tradeoff/conflict/resilience 중 하나),
            secondaryAxis(같은 값 또는 null), probeText, echoQuote, jdMatch(문자열 또는 null),
            strength(high/mid/low 중 하나).
            """;

    private final ChatClient chatClient;
    private final String systemPrompt;

    AnthropicProbeCandidateExtractorAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(loadAxesYaml(), MAX_CANDIDATES);
    }

    @Override
    public List<ProbeCandidateDraft> extract(String focusProject, List<String> portfolioChunks, List<String> jdKeywords) {
        String userMessage = buildUserMessage(focusProject, portfolioChunks, jdKeywords);

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

    private String buildUserMessage(String focusProject, List<String> portfolioChunks, List<String> jdKeywords) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(focusProject)) {
            sb.append("[집중 프로젝트]\n").append(focusProject).append("\n\n");
        }
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
                QuestionCandidateStrength.valueOf(entry.strength().toUpperCase()),
                null
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
