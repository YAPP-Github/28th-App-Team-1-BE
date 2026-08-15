package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewSessionAiCostJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

interface InterviewSessionAiCostJpaRepository extends JpaRepository<InterviewSessionAiCostJpaEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO interview_session_ai_cost (
                interview_session_id,
                anthropic_input_tokens, anthropic_output_tokens,
                anthropic_cache_write_tokens, anthropic_cache_read_tokens, anthropic_cost_nano_usd,
                openai_input_tokens, openai_output_tokens,
                openai_tts_characters, openai_stt_duration_millis, openai_cost_nano_usd,
                total_cost_nano_usd, call_count, created_at, updated_at
            ) VALUES (
                :sessionId,
                :anthropicInputTokens, :anthropicOutputTokens,
                :anthropicCacheWriteTokens, :anthropicCacheReadTokens, :anthropicCostNanoUsd,
                :openAiInputTokens, :openAiOutputTokens,
                :openAiTtsCharacters, :openAiSttDurationMillis, :openAiCostNanoUsd,
                :totalCostNanoUsd, 1, :now, :now
            )
            ON CONFLICT (interview_session_id) DO UPDATE SET
                anthropic_input_tokens = interview_session_ai_cost.anthropic_input_tokens
                        + EXCLUDED.anthropic_input_tokens,
                anthropic_output_tokens = interview_session_ai_cost.anthropic_output_tokens
                        + EXCLUDED.anthropic_output_tokens,
                anthropic_cache_write_tokens = interview_session_ai_cost.anthropic_cache_write_tokens
                        + EXCLUDED.anthropic_cache_write_tokens,
                anthropic_cache_read_tokens = interview_session_ai_cost.anthropic_cache_read_tokens
                        + EXCLUDED.anthropic_cache_read_tokens,
                anthropic_cost_nano_usd = interview_session_ai_cost.anthropic_cost_nano_usd
                        + EXCLUDED.anthropic_cost_nano_usd,
                openai_input_tokens = interview_session_ai_cost.openai_input_tokens
                        + EXCLUDED.openai_input_tokens,
                openai_output_tokens = interview_session_ai_cost.openai_output_tokens
                        + EXCLUDED.openai_output_tokens,
                openai_tts_characters = interview_session_ai_cost.openai_tts_characters
                        + EXCLUDED.openai_tts_characters,
                openai_stt_duration_millis = interview_session_ai_cost.openai_stt_duration_millis
                        + EXCLUDED.openai_stt_duration_millis,
                openai_cost_nano_usd = interview_session_ai_cost.openai_cost_nano_usd
                        + EXCLUDED.openai_cost_nano_usd,
                total_cost_nano_usd = interview_session_ai_cost.total_cost_nano_usd
                        + EXCLUDED.total_cost_nano_usd,
                call_count = interview_session_ai_cost.call_count + 1,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void accumulate(
            @Param("sessionId") Long sessionId,
            @Param("anthropicInputTokens") long anthropicInputTokens,
            @Param("anthropicOutputTokens") long anthropicOutputTokens,
            @Param("anthropicCacheWriteTokens") long anthropicCacheWriteTokens,
            @Param("anthropicCacheReadTokens") long anthropicCacheReadTokens,
            @Param("anthropicCostNanoUsd") long anthropicCostNanoUsd,
            @Param("openAiInputTokens") long openAiInputTokens,
            @Param("openAiOutputTokens") long openAiOutputTokens,
            @Param("openAiTtsCharacters") long openAiTtsCharacters,
            @Param("openAiSttDurationMillis") long openAiSttDurationMillis,
            @Param("openAiCostNanoUsd") long openAiCostNanoUsd,
            @Param("totalCostNanoUsd") long totalCostNanoUsd,
            @Param("now") LocalDateTime now
    );
}
