package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.PriorQaCache;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.portfolio.application.port.in.PortfolioChunkSearchUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioChunkResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 모델이 스키마 대신 산문을 반환해도 턴 제출이 죽지 않고 빈 분석 결과로 진행하는지 고정한다(#152).
class AnthropicLiveTurnAnalyzerAdapterParseFallbackTest {

    private static final String PROSE_RESPONSE = """
            I notice that the actual candidate answer text is missing from your request.
            Could you please provide the full transcript of what the candidate said?
            """;

    private AnthropicLiveTurnAnalyzerAdapter adapter(Function<Prompt, ChatResponse> behavior) {
        return new AnthropicLiveTurnAnalyzerAdapter(new StubChatModel(behavior), new NoOpChunkSearch(), new NoOpPriorQaCache());
    }

    private LiveTurnResult analyze(AnthropicLiveTurnAnalyzerAdapter adapter, String lastAnswer, TestType currentAxis) {
        return adapter.analyze(
                1L, null, "프로젝트를 간단히 소개해 주세요.", lastAnswer, currentAxis, JobType.BACKEND,
                List.of(), List.<QuestionCandidate>of(), Set.of()
        );
    }

    @Test
    void 응답이_스키마_밖_산문이면_빈_분석_결과로_진행한다() {
        LiveTurnResult result = analyze(adapter(prompt -> proseResponse()), "토큰 재등업 결합", TestType.DEPTH);

        assertThat(result.newProbes()).isEmpty();
        assertThat(result.staleUpdates()).isEmpty();
        assertThat(result.ceiling().reached()).isFalse();
        assertThat(result.ceiling().kind()).isNull();
    }

    @Test
    void 답변이_비어_있으면_LLM_호출_없이_빈_분석_결과를_반환한다() {
        LiveTurnResult result = analyze(adapter(prompt -> {
            throw new AssertionError("답변이 비면 LLM을 호출하지 않아야 해요.");
        }), "   ", TestType.DEPTH);

        assertThat(result.newProbes()).isEmpty();
        assertThat(result.staleUpdates()).isEmpty();
        assertThat(result.ceiling().reached()).isFalse();
    }

    @Test
    void 파싱_실패가_아닌_예외는_그대로_전파한다() {
        AnthropicLiveTurnAnalyzerAdapter adapter = adapter(prompt -> {
            throw new RuntimeException("연결이 끊겼어요.", new SocketTimeoutException("read timed out"));
        });

        assertThatThrownBy(() -> analyze(adapter, "충분히 긴 정상 답변입니다.", TestType.DEPTH))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("답변 분석(run_live_turn)에 실패했어요.");
    }

    private static ChatResponse proseResponse() {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(PROSE_RESPONSE))));
    }

    private record StubChatModel(Function<Prompt, ChatResponse> behavior) implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return behavior.apply(prompt);
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return AnthropicChatOptions.builder()
                    .model("claude-haiku-4-5-20251001")
                    .maxTokens(8192)
                    .thinking(AnthropicApi.ThinkingType.DISABLED, null)
                    .build();
        }
    }

    private static class NoOpChunkSearch implements PortfolioChunkSearchUseCase {
        @Override
        public List<PortfolioChunkResult> searchChunks(UUID portfolioId, String queryText, int topK) {
            return List.of();
        }

        @Override
        public List<PortfolioChunkResult> searchChunksWithoutThreshold(UUID portfolioId, String queryText, int topK) {
            return List.of();
        }
    }

    private static class NoOpPriorQaCache implements PriorQaCache {
        @Override
        public List<PriorTurn> get(Long sessionId, TestType axis) {
            return List.of();
        }

        @Override
        public void append(Long sessionId, TestType axis, PriorTurn turn) {
        }

        @Override
        public void clear(Long sessionId) {
        }
    }
}
