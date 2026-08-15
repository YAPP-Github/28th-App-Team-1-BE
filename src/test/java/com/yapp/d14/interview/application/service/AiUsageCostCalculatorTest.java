package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.properties.AiPricingProperties;
import com.yapp.d14.interview.application.command.AiUsageRecordCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageCostCalculatorTest {

    private static final String HAIKU = "claude-haiku-4-5-20251001";
    private static final String WHISPER = "whisper-1";
    private static final String TTS = "tts-1";

    private AiUsageCostCalculator aiUsageCostCalculator;

    @BeforeEach
    void setUp() {
        AiPricingProperties properties = new AiPricingProperties();
        properties.setModels(Map.of(
                HAIKU, modelPrice(1000, 5000, 1250, 100, 0, 0),
                WHISPER, modelPrice(0, 0, 0, 0, 0, 100_000),
                TTS, modelPrice(0, 0, 0, 0, 15_000, 0)
        ));
        aiUsageCostCalculator = new AiUsageCostCalculator(properties);
    }

    @Test
    @DisplayName("입력·출력 토큰에 각각의 단가를 적용해 합산한다")
    void 입력_출력_토큰_비용을_합산한다() {
        AiUsageRecordCommand command = AiUsageRecordCommand.anthropicChat(1L, HAIKU, 1_000, 200, 0, 0);

        long costNanoUsd = aiUsageCostCalculator.calculateNanoUsd(command);

        assertThat(costNanoUsd).isEqualTo(1_000 * 1000L + 200 * 5000L);
    }

    @Test
    @DisplayName("캐시 쓰기는 input의 1.25배, 캐시 읽기는 0.1배로 별도 계산한다")
    void 캐시_토큰은_별도_배수로_계산한다() {
        AiUsageRecordCommand command = AiUsageRecordCommand.anthropicChat(1L, HAIKU, 0, 0, 10_000, 10_000);

        long costNanoUsd = aiUsageCostCalculator.calculateNanoUsd(command);

        assertThat(costNanoUsd).isEqualTo(10_000 * 1250L + 10_000 * 100L);
    }

    @Test
    @DisplayName("같은 토큰 수라도 캐시 읽기는 input 대비 10분의 1로 계산된다")
    void 캐시_읽기는_input의_10분의_1이다() {
        long inputCost = aiUsageCostCalculator.calculateNanoUsd(
                AiUsageRecordCommand.anthropicChat(1L, HAIKU, 10_000, 0, 0, 0));
        long cacheReadCost = aiUsageCostCalculator.calculateNanoUsd(
                AiUsageRecordCommand.anthropicChat(1L, HAIKU, 0, 0, 0, 10_000));

        assertThat(cacheReadCost * 10).isEqualTo(inputCost);
    }

    @Test
    @DisplayName("TTS는 입력 문자 수로 계산한다")
    void tts는_문자_수로_계산한다() {
        long costNanoUsd = aiUsageCostCalculator.calculateNanoUsd(
                AiUsageRecordCommand.openAiTts(1L, TTS, 300));

        assertThat(costNanoUsd).isEqualTo(300 * 15_000L);
    }

    @Test
    @DisplayName("STT는 밀리초를 초 단가로 환산해 계산한다")
    void stt는_오디오_길이로_계산한다() {
        long costNanoUsd = aiUsageCostCalculator.calculateNanoUsd(
                AiUsageRecordCommand.openAiStt(1L, WHISPER, 90_000));

        assertThat(costNanoUsd).isEqualTo(90 * 100_000L);
    }

    @Test
    @DisplayName("단가가 등록되지 않은 모델은 0으로 계산한다")
    void 단가_미등록_모델은_0이다() {
        AiUsageRecordCommand command = AiUsageRecordCommand.anthropicChat(1L, "claude-unknown", 1_000, 1_000, 0, 0);

        assertThat(aiUsageCostCalculator.calculateNanoUsd(command)).isZero();
    }

    @Test
    @DisplayName("모델 ID 대소문자가 달라도 같은 단가를 찾는다")
    void 모델_ID는_대소문자를_가리지_않는다() {
        long costNanoUsd = aiUsageCostCalculator.calculateNanoUsd(
                AiUsageRecordCommand.anthropicChat(1L, HAIKU.toUpperCase(), 1_000, 0, 0, 0));

        assertThat(costNanoUsd).isEqualTo(1_000 * 1000L);
    }

    private AiPricingProperties.ModelPrice modelPrice(
            long input, long output, long cacheWrite, long cacheRead, long perCharacter, long perSecond
    ) {
        AiPricingProperties.ModelPrice price = new AiPricingProperties.ModelPrice();
        price.setInputNanoUsdPerToken(input);
        price.setOutputNanoUsdPerToken(output);
        price.setCacheWriteNanoUsdPerToken(cacheWrite);
        price.setCacheReadNanoUsdPerToken(cacheRead);
        price.setNanoUsdPerCharacter(perCharacter);
        price.setNanoUsdPerSecond(perSecond);
        return price;
    }
}
