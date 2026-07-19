package com.yapp.d14.jd.application.service;

import com.yapp.d14.jd.application.command.JdValidateCommand;
import com.yapp.d14.jd.application.port.in.JdCrawlResult;
import com.yapp.d14.jd.application.port.in.JdValidateUseCase;
import com.yapp.d14.jd.application.port.in.JdValidationFailureReason;
import com.yapp.d14.jd.application.port.out.JdContentExtractor;
import com.yapp.d14.jd.application.port.out.JdContentFetcher;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import com.yapp.d14.jd.application.port.out.JdValidationRateLimiter;
import com.yapp.d14.jd.exception.JdCrawlingFailedException;
import com.yapp.d14.jd.exception.JdErrorCode;
import com.yapp.d14.jd.exception.JdException;
import com.yapp.d14.jd.exception.JdExtractionFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class JdValidateService implements JdValidateUseCase {

    private static final int MIN_CONTENT_LENGTH = 200;
    private static final int MAX_DAILY_VALIDATION_COUNT = 5;

    private final JdContentFetcher jdContentFetcher;
    private final JdContentExtractor jdContentExtractor;
    private final JdContentRepository jdContentRepository;
    private final JdValidationRateLimiter jdValidationRateLimiter;

    @Override
    public JdCrawlResult validate(JdValidateCommand command) {
        Optional<String> cached = jdContentRepository.get(command.jdUrl());
        if (cached.isPresent()) {
            return JdCrawlResult.success(cached.get());
        }

        if (jdValidationRateLimiter.getTodayCount(command.userId()) >= MAX_DAILY_VALIDATION_COUNT) {
            throw new JdException(JdErrorCode.JD_VALIDATION_LIMIT_EXCEEDED);
        }

        String rawContent;
        try {
            rawContent = jdContentFetcher.fetch(command.jdUrl());
        } catch (JdCrawlingFailedException e) {
            return JdCrawlResult.failure(JdValidationFailureReason.CRAWLING_FAILED);
        }

        if (rawContent.length() < MIN_CONTENT_LENGTH) {
            return JdCrawlResult.failure(JdValidationFailureReason.CONTENT_TOO_SHORT);
        }

        String extractedContent;
        try {
            extractedContent = jdContentExtractor.extract(rawContent);
        } catch (JdExtractionFailedException e) {
            return JdCrawlResult.failure(JdValidationFailureReason.EXTRACTION_FAILED);
        }

        jdContentRepository.save(command.jdUrl(), extractedContent);
        jdValidationRateLimiter.increment(command.userId());
        return JdCrawlResult.success(extractedContent);
    }
}
