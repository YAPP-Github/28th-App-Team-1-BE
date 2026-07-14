package com.yapp.d14.jd.application.service;

import com.yapp.d14.jd.application.command.JdValidateCommand;
import com.yapp.d14.jd.application.port.in.JdCrawlResult;
import com.yapp.d14.jd.application.port.in.JdValidateUseCase;
import com.yapp.d14.jd.application.port.in.JdValidationFailureReason;
import com.yapp.d14.jd.application.port.out.JdContentExtractor;
import com.yapp.d14.jd.application.port.out.JdContentFetcher;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import com.yapp.d14.jd.exception.JdCrawlingFailedException;
import com.yapp.d14.jd.exception.JdExtractionFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class JdValidateService implements JdValidateUseCase {

    private static final int MIN_CONTENT_LENGTH = 200;

    private final JdContentFetcher jdContentFetcher;
    private final JdContentExtractor jdContentExtractor;
    private final JdContentRepository jdContentRepository;

    @Override
    public JdCrawlResult validate(JdValidateCommand command) {
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
        return JdCrawlResult.success(extractedContent);
    }
}
