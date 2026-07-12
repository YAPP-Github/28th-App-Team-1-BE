package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InterviewSessionCreateValidatorTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private JdContentRepository jdContentRepository;

    @Mock
    private PortfolioEmbeddingStore portfolioEmbeddingStore;

    @InjectMocks
    private InterviewSessionCreateValidator validator;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    private InterviewSessionCreateCommand command(String jdUrl, String jdText, String freeText) {
        return new InterviewSessionCreateCommand(userId, portfolioId, JobType.BACKEND, 3, jdUrl, jdText, freeText);
    }

    private Portfolio readyPortfolio() {
        Portfolio portfolio = Portfolio.create(portfolioId, userId, "resume.pdf", 1024, 5, "users/x/portfolios/x/x.pdf");
        portfolio.ready();
        return portfolio;
    }

    @Test
    void 포트폴리오가_없으면_PORTFOLIO_NOT_FOUND() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }

    @Test
    void 포트폴리오_소유자가_다르면_PORTFOLIO_NOT_FOUND() {
        Portfolio othersPortfolio = Portfolio.create(portfolioId, UUID.randomUUID(), "resume.pdf", 1024, 5, "key");
        othersPortfolio.ready();
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(othersPortfolio));

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }

    @Test
    void 포트폴리오가_PROCESSING이면_PORTFOLIO_PROCESSING() {
        Portfolio portfolio = Portfolio.create(portfolioId, userId, "resume.pdf", 1024, 5, "key");
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_PROCESSING);
    }

    @Test
    void 포트폴리오가_FAILED_FILE이면_PORTFOLIO_UPLOAD_FAILED() {
        Portfolio portfolio = Portfolio.create(portfolioId, userId, "resume.pdf", 1024, 5, "key");
        portfolio.failFile("파일이 손상됨");
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
    }

    @Test
    void 포트폴리오가_FAILED_SYSTEM이면_PORTFOLIO_UPLOAD_FAILED() {
        Portfolio portfolio = Portfolio.create(portfolioId, userId, "resume.pdf", 1024, 5, "key");
        portfolio.failSystem("시스템 오류");
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
    }

    @Test
    void 포트폴리오가_READY이고_JD_freeText_모두_없으면_통과한다() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));

        assertThatCode(() -> validator.validate(command(null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void jdUrl이_있고_캐시가_존재하면_통과한다() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));
        given(jdContentRepository.exists("https://example.com/jd")).willReturn(true);

        assertThatCode(() -> validator.validate(command("https://example.com/jd", null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void jdUrl이_있고_캐시가_없으면_JD_NOT_VALIDATED() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));
        given(jdContentRepository.exists("https://example.com/jd")).willReturn(false);

        assertThatThrownBy(() -> validator.validate(command("https://example.com/jd", null, null)))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.JD_NOT_VALIDATED);
    }

    @Test
    void jdText가_200자_미만이면_INVALID_JD_LENGTH() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));

        assertThatThrownBy(() -> validator.validate(command(null, "가".repeat(199), null)))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_JD_LENGTH);
    }

    @Test
    void jdText가_3000자_초과면_INVALID_JD_LENGTH() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));

        assertThatThrownBy(() -> validator.validate(command(null, "가".repeat(3001), null)))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_JD_LENGTH);
    }

    @Test
    void jdText가_200_3000자_경계값이면_통과한다() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));

        assertThatCode(() -> validator.validate(command(null, "가".repeat(200), null))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(command(null, "가".repeat(3000), null))).doesNotThrowAnyException();
    }

    @Test
    void freeText가_10자_미만이면_INVALID_FREETEXT_LENGTH() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(9))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_FREETEXT_LENGTH);
    }

    @Test
    void freeText가_300자_초과면_INVALID_FREETEXT_LENGTH() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(301))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_FREETEXT_LENGTH);
    }

    @Test
    void freeText가_유효길이여도_연관성이_0_6미만이면_FREETEXT_NOT_RELEVANT() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));
        given(portfolioEmbeddingStore.findTopSimilarityScore(any(), any())).willReturn(Optional.of(0.59));

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(20))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.FREETEXT_NOT_RELEVANT);
    }

    @Test
    void freeText_연관성_점수가_없으면_0점으로_취급해_FREETEXT_NOT_RELEVANT() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));
        given(portfolioEmbeddingStore.findTopSimilarityScore(any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(20))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.FREETEXT_NOT_RELEVANT);
    }

    @Test
    void freeText가_유효길이이고_연관성이_0_6이상이면_통과한다() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.of(readyPortfolio()));
        given(portfolioEmbeddingStore.findTopSimilarityScore(any(), any())).willReturn(Optional.of(0.6));

        assertThatCode(() -> validator.validate(command(null, null, "가".repeat(20)))).doesNotThrowAnyException();
    }

    @Test
    void 포트폴리오_검증이_JD_검증보다_먼저_실행된다() {
        given(portfolioRepository.findById(portfolioId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(command(null, "가".repeat(1), null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }
}
