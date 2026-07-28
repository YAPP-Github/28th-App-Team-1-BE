package com.yapp.d14.interview.application.service;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
@UtilityClass
class LlmCallRetrySupport {

    <T> T retry(Supplier<T> call, int maxRetries, String logTag) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("[{}] LLM 호출 실패 ({}/{})", logTag, attempt, maxRetries + 1, e);
            }
        }
        throw lastError;
    }
}
