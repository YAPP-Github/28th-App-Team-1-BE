package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 이전 면접 질문 이력은 세션마다 달라지므로 시스템 프롬프트가 아니라 유저 메시지에 실어야 한다(#133).
// 시스템 프롬프트에 넣으면 전 세션이 공유하던 프롬프트 캐시 프리픽스가 세션 단위로 쪼개진다.
class PriorQuestionPromptSectionTest {

    private static final String SECTION_HEADER = "[이전 면접에서 이미 물어본 질문]";

    @Test
    void 캐물지점_추출_유저_메시지에_이전_질문_섹션을_싣는다() {
        String userMessage = AnthropicProbeCandidateExtractorAdapter.buildUserMessage(
                "결제 시스템", List.of("포폴 청크1"), List.of("키워드1"),
                List.of("분산락을 왜 그렇게 잡으셨나요?", "타임세일 트래픽은 어떻게 감당하셨나요?")
        );

        assertThat(userMessage)
                .contains(SECTION_HEADER)
                .contains("- 분산락을 왜 그렇게 잡으셨나요?")
                .contains("- 타임세일 트래픽은 어떻게 감당하셨나요?");
    }

    @Test
    void 이전_질문이_없으면_캐물지점_추출_유저_메시지에서_섹션을_생략한다() {
        assertThat(AnthropicProbeCandidateExtractorAdapter.buildUserMessage(
                "결제 시스템", List.of("포폴 청크1"), List.of("키워드1"), List.of()
        )).doesNotContain(SECTION_HEADER);

        assertThat(AnthropicProbeCandidateExtractorAdapter.buildUserMessage(
                "결제 시스템", List.of("포폴 청크1"), List.of("키워드1"), null
        )).doesNotContain(SECTION_HEADER);
    }

    @Test
    void 여는_질문_유저_메시지에_이전_질문_섹션을_싣는다() {
        String userMessage = AnthropicQuestionTextGeneratorAdapter.buildOpenerUserMessage(
                TestType.DEPTH, JobType.BACKEND, 3, List.of("대용량 트래픽"), List.of("포폴 청크1"),
                List.of("최근 가장 깊게 파고든 부분이 있을까요?")
        );

        assertThat(userMessage)
                .contains(SECTION_HEADER)
                .contains("- 최근 가장 깊게 파고든 부분이 있을까요?");
    }

    @Test
    void 이전_질문이_없으면_여는_질문_유저_메시지에서_섹션을_생략한다() {
        assertThat(AnthropicQuestionTextGeneratorAdapter.buildOpenerUserMessage(
                TestType.DEPTH, JobType.BACKEND, 3, List.of(), List.of(), List.of()
        )).doesNotContain(SECTION_HEADER);

        assertThat(AnthropicQuestionTextGeneratorAdapter.buildOpenerUserMessage(
                TestType.DEPTH, JobType.BACKEND, 3, List.of(), List.of(), null
        )).doesNotContain(SECTION_HEADER);
    }
}
