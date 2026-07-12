package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AxisWeightCalculator;
import com.yapp.d14.interview.domain.AxisWeightCalculator.AxisAssignment;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import com.yapp.d14.ticket.application.port.in.TicketAvailabilityCheckUseCase;
import com.yapp.d14.ticket.application.port.in.TicketHoldUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
class InterviewSessionCreateService implements InterviewSessionCreateUseCase {

    private static final int MIN_JD_TEXT_LENGTH = 200;
    private static final int MAX_JD_TEXT_LENGTH = 3000;
    private static final int MIN_FREE_TEXT_LENGTH = 10;
    private static final int MAX_FREE_TEXT_LENGTH = 300;
    private static final double FREE_TEXT_RELEVANCE_THRESHOLD = 0.6;

    private final TicketAvailabilityCheckUseCase ticketAvailabilityCheckUseCase;
    private final TicketHoldUseCase ticketHoldUseCase;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;
    private final JdContentRepository jdContentRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewAxisPlanRepository interviewAxisPlanRepository;

    @Override
    @Transactional
    public InterviewSessionCreateResult create(InterviewSessionCreateCommand command) {
        ticketAvailabilityCheckUseCase.checkAvailable(command.userId());

        validatePortfolio(command);
        validateJd(command);
        validateFreeText(command);

        Map<TestType, Integer> weights = AxisWeightCalculator.compute(command.jobRole(), command.careerYears());
        Map<TestType, AxisAssignment> assignments = AxisWeightCalculator.assignTierAndBudget(weights);

        InterviewSession session = InterviewSession.create(
                command.userId(),
                command.portfolioId(),
                command.jobRole(),
                command.careerYears(),
                command.jdUrl(),
                command.jdText(),
                command.freeText()
        );
        session.assignWeights(weights);
        session = interviewSessionRepository.save(session);

        for (TestType testType : TestType.values()) {
            AxisAssignment assignment = assignments.get(testType);
            interviewAxisPlanRepository.save(
                    InterviewAxisPlan.create(session.getId(), testType, assignment.tier(), assignment.budget())
            );
        }

        ticketHoldUseCase.hold(command.userId(), session.getId());

        return new InterviewSessionCreateResult(session.getId(), session.getStatus());
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
            if (!jdContentRepository.exists(command.userId(), command.jdUrl())) {
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
