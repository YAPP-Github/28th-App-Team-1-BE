package com.yapp.d14.interview.application.service;

import com.yapp.d14.consent.application.port.in.ConsentUpToDateCheckUseCase;
import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.jd.application.port.in.JdValidationCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioSimilarityCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import com.yapp.d14.user.application.port.in.AccountStatusCheckUseCase;
import com.yapp.d14.user.application.port.in.FindUserUseCase;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class InterviewSessionCreateValidator {

    private static final int MIN_JD_TEXT_LENGTH = 200;
    private static final int MAX_JD_TEXT_LENGTH = 3000;
    private static final int MIN_FREE_TEXT_LENGTH = 10;
    private static final int MAX_FREE_TEXT_LENGTH = 300;
    private static final double FREE_TEXT_RELEVANCE_THRESHOLD = 0.4;

    private final PortfolioStatusUseCase portfolioStatusUseCase;
    private final JdValidationCheckUseCase jdValidationCheckUseCase;
    private final PortfolioSimilarityCheckUseCase portfolioSimilarityCheckUseCase;
    private final FindUserUseCase findUserUseCase;
    private final AccountStatusCheckUseCase accountStatusCheckUseCase;
    private final ConsentUpToDateCheckUseCase consentUpToDateCheckUseCase;

    // 면접 시작 게이트는 이용권 PRD의 번호 순서대로 통과시킨다. 정지와 동의 구버전이 겹치면
    // 게이트1이 먼저 걸려 A4(이용 제한)가 뜬다 — PRD Part7 5장 엣지 케이스.
    // 게이트4(NO_REMAINING)는 이 검증을 통과한 뒤 InterviewSessionCreateService가 이용권 모듈에 위임한다.
    InterviewSessionCreateContext validate(InterviewSessionCreateCommand command) {
        accountStatusCheckUseCase.checkNotSuspended(command.userId());
        consentUpToDateCheckUseCase.checkUpToDate(command.userId());

        String portfolioFileName = validatePortfolio(command);
        validateJd(command);
        validateFreeText(command);
        User user = requireRegisteredProfile(command.userId());
        return new InterviewSessionCreateContext(portfolioFileName, JobType.valueOf(user.getJobRole().name()), user.getCareerYears());
    }

    private User requireRegisteredProfile(UUID userId) {
        User user = findUserUseCase.findById(userId);
        if (user.getName() == null || user.getJobRole() == null || user.getCareerYears() == null) {
            throw new InterviewException(InterviewErrorCode.USER_PROFILE_NOT_REGISTERED);
        }
        return user;
    }

    private String validatePortfolio(InterviewSessionCreateCommand command) {
        PortfolioStatusResult status = portfolioStatusUseCase.getStatus(command.userId(), command.portfolioId());

        switch (status.status()) {
            case READY -> { }
            case PROCESSING -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_PROCESSING);
            case FAILED_FILE, FAILED_SYSTEM, CANCELLED -> throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
        }

        return status.fileName();
    }

    private void validateJd(InterviewSessionCreateCommand command) {
        if (StringUtils.hasText(command.jdUrl()) && StringUtils.hasText(command.jdText())) {
            throw new InterviewException(InterviewErrorCode.JD_URL_AND_TEXT_BOTH_PROVIDED);
        }

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
