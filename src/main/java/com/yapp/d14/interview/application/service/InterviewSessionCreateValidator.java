package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.jd.application.port.in.JdValidationCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioSimilarityCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
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

    private final PortfolioStatusUseCase portfolioStatusUseCase;
    private final JdValidationCheckUseCase jdValidationCheckUseCase;
    private final PortfolioSimilarityCheckUseCase portfolioSimilarityCheckUseCase;

    void validate(InterviewSessionCreateCommand command) {
        validatePortfolio(command);
        validateJd(command);
        validateFreeText(command);
    }

    private void validatePortfolio(InterviewSessionCreateCommand command) {
        PortfolioStatusResult status = portfolioStatusUseCase.getStatus(command.userId(), command.portfolioId());

        switch (status.status()) {
            case READY -> { }
            case PROCESSING -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_PROCESSING);
            case FAILED_FILE, FAILED_SYSTEM -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
        }
    }

    private void validateJd(InterviewSessionCreateCommand command) {
        if (StringUtils.hasText(command.jdUrl())) {
            if (!jdValidationCheckUseCase.isValidated(command.jdUrl())) {
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

        double score = portfolioSimilarityCheckUseCase.checkSimilarity(command.portfolioId(), command.freeText())
                .orElse(0.0);
        if (score < FREE_TEXT_RELEVANCE_THRESHOLD) {
            throw new InterviewException(InterviewErrorCode.FREETEXT_NOT_RELEVANT);
        }
    }
}
