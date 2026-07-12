package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
class InterviewSessionCreateValidator {

    private static final int MIN_JD_TEXT_LENGTH = 200;
    private static final int MAX_JD_TEXT_LENGTH = 3000;
    private static final int MIN_FREE_TEXT_LENGTH = 10;
    private static final int MAX_FREE_TEXT_LENGTH = 300;
    private static final double FREE_TEXT_RELEVANCE_THRESHOLD = 0.6;

    private final PortfolioRepository portfolioRepository;
    private final JdContentRepository jdContentRepository;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;

    void validate(InterviewSessionCreateCommand command) {
        validatePortfolio(command);
        validateJd(command);
        validateFreeText(command);
    }

    private void validatePortfolio(InterviewSessionCreateCommand command) {
        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
                .filter(p -> p.getUserId().equals(command.userId()))
                .orElseThrow(() -> new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        switch (portfolio.getStatus()) {
            case READY -> { }
            case PROCESSING -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_PROCESSING);
            case FAILED_FILE, FAILED_SYSTEM -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
        }
    }

    private void validateJd(InterviewSessionCreateCommand command) {
        if (StringUtils.hasText(command.jdUrl())) {
            if (!jdContentRepository.exists(command.jdUrl())) {
                throw new InterviewException(InterviewErrorCode.JD_NOT_VALIDATED);
            }
            return;
        }

        if (StringUtils.hasText(command.jdText())) {
            int length = command.jdText().trim().length();
            if (length < MIN_JD_TEXT_LENGTH || length > MAX_JD_TEXT_LENGTH) {
                throw new InterviewException(InterviewErrorCode.INVALID_JD_LENGTH);
            }
        }
    }

    private void validateFreeText(InterviewSessionCreateCommand command) {
        if (!StringUtils.hasText(command.freeText())) {
            return;
        }

        int length = command.freeText().trim().length();
        if (length < MIN_FREE_TEXT_LENGTH || length > MAX_FREE_TEXT_LENGTH) {
            throw new InterviewException(InterviewErrorCode.INVALID_FREETEXT_LENGTH);
        }

        double score = portfolioEmbeddingStore.findTopSimilarityScore(command.portfolioId(), command.freeText())
                .orElse(0.0);
        if (score < FREE_TEXT_RELEVANCE_THRESHOLD) {
            throw new InterviewException(InterviewErrorCode.FREETEXT_NOT_RELEVANT);
        }
    }
}
