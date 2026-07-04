package com.yapp.d14.jd.application.service;

import com.yapp.d14.jd.application.command.JdValidateCommand;
import com.yapp.d14.jd.application.port.in.JdCrawlResult;
import com.yapp.d14.jd.application.port.in.JdValidateUseCase;
import com.yapp.d14.jd.application.port.in.JdValidationFailureReason;
import com.yapp.d14.jd.application.port.out.JdContentCache;
import com.yapp.d14.jd.application.port.out.JdContentFetcher;
import com.yapp.d14.jd.exception.JdCrawlingFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class JdValidateService implements JdValidateUseCase {

    private static final int MIN_CONTENT_LENGTH = 200;

    private final JdContentFetcher jdContentFetcher;
    private final JdContentCache jdContentCache;

    @Override
    public JdCrawlResult validate(JdValidateCommand command) {
        String content;
        try {
            content = jdContentFetcher.fetch(command.jdUrl());
        } catch (JdCrawlingFailedException e) {
            return JdCrawlResult.failure(JdValidationFailureReason.CRAWLING_FAILED);
        }

        if (content.length() < MIN_CONTENT_LENGTH) {
            return JdCrawlResult.failure(JdValidationFailureReason.CONTENT_TOO_SHORT);
        }

        jdContentCache.save(command.userId(), command.jdUrl(), content);
        return JdCrawlResult.success(content);
    }
}
