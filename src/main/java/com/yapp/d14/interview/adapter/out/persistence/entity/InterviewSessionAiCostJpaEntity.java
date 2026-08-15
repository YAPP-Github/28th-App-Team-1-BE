package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.InterviewSessionAiCost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_session_ai_cost")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSessionAiCostJpaEntity {

    @Id
    @Column(name = "interview_session_id")
    private Long interviewSessionId;

    @Column(name = "anthropic_input_tokens", nullable = false)
    private long anthropicInputTokens;

    @Column(name = "anthropic_output_tokens", nullable = false)
    private long anthropicOutputTokens;

    @Column(name = "anthropic_cache_write_tokens", nullable = false)
    private long anthropicCacheWriteTokens;

    @Column(name = "anthropic_cache_read_tokens", nullable = false)
    private long anthropicCacheReadTokens;

    @Column(name = "anthropic_cost_nano_usd", nullable = false)
    private long anthropicCostNanoUsd;

    @Column(name = "openai_input_tokens", nullable = false)
    private long openAiInputTokens;

    @Column(name = "openai_output_tokens", nullable = false)
    private long openAiOutputTokens;

    @Column(name = "openai_tts_characters", nullable = false)
    private long openAiTtsCharacters;

    @Column(name = "openai_stt_duration_millis", nullable = false)
    private long openAiSttDurationMillis;

    @Column(name = "openai_cost_nano_usd", nullable = false)
    private long openAiCostNanoUsd;

    @Column(name = "total_cost_nano_usd", nullable = false)
    private long totalCostNanoUsd;

    @Column(name = "call_count", nullable = false)
    private int callCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InterviewSessionAiCost toDomain() {
        return InterviewSessionAiCost.of(
                interviewSessionId,
                anthropicInputTokens,
                anthropicOutputTokens,
                anthropicCacheWriteTokens,
                anthropicCacheReadTokens,
                anthropicCostNanoUsd,
                openAiInputTokens,
                openAiOutputTokens,
                openAiTtsCharacters,
                openAiSttDurationMillis,
                openAiCostNanoUsd,
                totalCostNanoUsd,
                callCount,
                createdAt,
                updatedAt
        );
    }
}
