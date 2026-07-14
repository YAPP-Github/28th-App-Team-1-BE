package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.CeilingAssessment;
import com.yapp.d14.interview.application.port.out.LiveTurnAnalyzer;
import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.StaleProbeUpdate;
import com.yapp.d14.interview.domain.JobType;
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
class AnthropicLiveTurnAnalyzerAdapter implements LiveTurnAnalyzer {

    private static final String AXES_YAML_PATH = "interview-rubric/axes.yaml";
    private static final String PRINCIPLES_YAML_PATH = "interview-rubric/principles.yaml";
    // TODO: ceiling-fewshot.md는 천장 판별 로직을 실제로 구현할 때(매 턴 일반화 이슈) 시스템 프롬프트에 추가한다.

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 AI 면접관입니다. 지원자의 방금 답변에서 더 파고들어 물어볼 만한 "캐물지점" 후보를 추출합니다.

            아래 6대 평가 항목(axis) 정의를 기준으로 각 캐물지점에 axis 태그를 답니다.
            %s

            아래는 캐물지점을 만들 때 참고할 전술 목록(P1~P24)과 직군별 필수/권장 매트릭스입니다.
            사용자 메시지에 명시된 jobRole에 해당하는 job_role_profiles 항목을 확인해,
            REQUIRED로 표시된 P21~P24 전술을 우선 적용하고 그 다음 RECOMMENDED로 확장하세요.
            %s

            규칙:
            - 캐물지점은 반드시 방금 답변 내용에 근거해야 합니다.
            - probeText는 "무엇을 캐물을지"에 대한 내부 메모(질문 문장 아님), echoQuote는 질문할 때 그대로 되받아 물을 원 표현입니다.
            - strength는 신호가 진할수록 high, 약하면 low로 답니다.
            - 개수를 인위적으로 채우거나 줄이지 마세요. 답변에서 자연스럽게 나오는 만큼만 뽑습니다.

            출력은 다른 설명 없이 JSON 배열 하나만 반환하세요. 각 원소는 다음 필드를 가집니다:
            axis(depth/boundary/connection/tradeoff/conflict/resilience 중 하나),
            secondaryAxis(같은 값 또는 null), probeText, echoQuote, jdMatch(문자열 또는 null),
            strength(high/mid/low 중 하나).
            """;

    private final ChatClient chatClient;
    private final String systemPrompt;

    AnthropicLiveTurnAnalyzerAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(loadResource(AXES_YAML_PATH), loadResource(PRINCIPLES_YAML_PATH));
    }

    @Override
    public LiveTurnResult analyze(
            Long sessionId,
            String lastQuestion,
            String lastAnswer,
            TestType currentAxis,
            JobType jobRole,
            List<PriorTurn> priorQa
    ) {
        // TODO: search_portfolio/read_project_detail/get_prior_qa tool calling 미구현.
        //       포트폴리오 대조 기반 모순 감지·근거 보강은 매 턴 일반화 이슈에서 추가한다.
        // TODO: principle_used 추적 - QuestionCandidate에 principleUsed 필드 없음.
        //       last_question이 어느 원칙에서 나왔는지 추적해야 모델이
        //       전술 중복을 피하고 사다리 타듯 이어갈 수 있다 (매 턴 일반화 이슈에서 처리).
        String userMessage = buildUserMessage(lastQuestion, lastAnswer, jobRole);

        try {
            List<ProbeCandidateLlmEntry> entries = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .entity(new ParameterizedTypeReference<List<ProbeCandidateLlmEntry>>() {
                    });
            List<ProbeCandidateDraft> newProbes = entries.stream().map(this::toDraft).toList();

            // TODO: 천장 판별(ceiling)은 current_axis가 있는 일반 매 턴 루프에서만 의미가 있다.
            //       매 턴 일반화 이슈에서 ceiling-fewshot.md를 프롬프트에 추가하고 실제 판별 로직을 연결한다.
            CeilingAssessment ceiling = new CeilingAssessment(false, null, "current_axis 없음 - 판별 대상 아님");
            // TODO: 모순 감지(stale_updates)는 prior_qa 대조가 필요하다. 매 턴 일반화 이슈에서 연결한다.
            List<StaleProbeUpdate> staleUpdates = List.of();

            return new LiveTurnResult(newProbes, ceiling, staleUpdates);
        } catch (Exception e) {
            log.error("[LIVE TURN ANALYZE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("답변 분석(run_live_turn)에 실패했어요.", e);
        }
    }

    private String buildUserMessage(String lastQuestion, String lastAnswer, JobType jobRole) {
        return """
                [직무] %s
                [방금 질문] %s
                [방금 답변] %s
                """.formatted(jobRole, lastQuestion, lastAnswer);
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

    private static String loadResource(String path) {
        try {
            return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(path + " 로드에 실패했어요.", e);
        }
    }
}
