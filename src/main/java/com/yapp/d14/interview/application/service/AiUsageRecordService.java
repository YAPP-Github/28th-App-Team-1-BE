package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.AiUsageRecordCommand;
import com.yapp.d14.interview.application.port.in.AiUsageRecordUseCase;
import com.yapp.d14.interview.application.port.out.AiCostDelta;
import com.yapp.d14.interview.application.port.out.InterviewSessionAiCostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class AiUsageRecordService implements AiUsageRecordUseCase {

    private final InterviewSessionAiCostRepository interviewSessionAiCostRepository;
    private final AiUsageCostCalculator aiUsageCostCalculator;

    @Override
    @Async("aiUsageTaskExecutor")
    public void record(AiUsageRecordCommand command) {
        if (command == null || command.sessionId() == null) {
            return;
        }
        try {
            long costNanoUsd = aiUsageCostCalculator.calculateNanoUsd(command);
            interviewSessionAiCostRepository.accumulate(new AiCostDelta(
                    command.sessionId(),
                    command.provider(),
                    command.inputTokens(),
                    command.outputTokens(),
                    command.cacheWriteTokens(),
                    command.cacheReadTokens(),
                    command.ttsCharacters(),
                    command.sttDurationMillis(),
                    costNanoUsd
            ));
        } catch (Exception e) {
            log.warn("[AI USAGE] 사용량 기록 실패, 본 흐름에는 영향 없어요 - sessionId={}, provider={}, model={}",
                    command.sessionId(), command.provider(), command.model(), e);
        }
    }
}
