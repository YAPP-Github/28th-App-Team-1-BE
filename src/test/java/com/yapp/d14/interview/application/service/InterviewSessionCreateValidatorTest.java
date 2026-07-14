package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.jd.application.port.in.JdValidationCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioSimilarityCheckUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
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
    private PortfolioStatusUseCase portfolioStatusUseCase;

    @Mock
    private JdValidationCheckUseCase jdValidationCheckUseCase;

    @Mock
    private PortfolioSimilarityCheckUseCase portfolioSimilarityCheckUseCase;

    @InjectMocks
    private InterviewSessionCreateValidator validator;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    private InterviewSessionCreateCommand command(String jdUrl, String jdText, String freeText) {
        return new InterviewSessionCreateCommand(userId, portfolioId, JobType.BACKEND, 3, jdUrl, jdText, freeText);
    }

    private void givenPortfolioStatus(PortfolioStatus status) {
        given(portfolioStatusUseCase.getStatus(userId, portfolioId))
                .willReturn(new PortfolioStatusResult(portfolioId, status, "메시지"));
    }

    @Test
    void 포트폴리오가_없거나_본인_소유가_아니면_PORTFOLIO_NOT_FOUND() {
        given(portfolioStatusUseCase.getStatus(userId, portfolioId))
                .willThrow(new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }

    @Test
    void 포트폴리오가_PROCESSING이면_PORTFOLIO_PROCESSING() {
        givenPortfolioStatus(PortfolioStatus.PROCESSING);

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_PROCESSING);
    }

    @Test
    void 포트폴리오가_FAILED_FILE이면_PORTFOLIO_UPLOAD_FAILED() {
        givenPortfolioStatus(PortfolioStatus.FAILED_FILE);

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
    }

    @Test
    void 포트폴리오가_FAILED_SYSTEM이면_PORTFOLIO_UPLOAD_FAILED() {
        givenPortfolioStatus(PortfolioStatus.FAILED_SYSTEM);

        assertThatThrownBy(() -> validator.validate(command(null, null, null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_UPLOAD_FAILED);
    }

    @Test
    void 포트폴리오가_READY이고_JD_freeText_모두_없으면_통과한다() {
        givenPortfolioStatus(PortfolioStatus.READY);

        assertThatCode(() -> validator.validate(command(null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void jdUrl이_있고_캐시가_존재하면_통과한다() {
        givenPortfolioStatus(PortfolioStatus.READY);
        given(jdValidationCheckUseCase.isValidated("https://example.com/jd")).willReturn(true);

        assertThatCode(() -> validator.validate(command("https://example.com/jd", null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void jdUrl이_있고_캐시가_없으면_JD_NOT_VALIDATED() {
        givenPortfolioStatus(PortfolioStatus.READY);
        given(jdValidationCheckUseCase.isValidated("https://example.com/jd")).willReturn(false);

        assertThatThrownBy(() -> validator.validate(command("https://example.com/jd", null, null)))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.JD_NOT_VALIDATED);
    }

    @Test
    void jdUrl과_jdText가_함께_있으면_JD_URL_AND_TEXT_BOTH_PROVIDED() {
        givenPortfolioStatus(PortfolioStatus.READY);

        assertThatThrownBy(() -> validator.validate(command("https://example.com/jd", "가".repeat(200), null)))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.JD_URL_AND_TEXT_BOTH_PROVIDED);
    }

    @Test
    void jdText가_200자_미만이면_INVALID_JD_LENGTH() {
        givenPortfolioStatus(PortfolioStatus.READY);

        assertThatThrownBy(() -> validator.validate(command(null, "가".repeat(199), null)))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_JD_LENGTH);
    }

    @Test
    void jdText가_3000자_초과면_INVALID_JD_LENGTH() {
        givenPortfolioStatus(PortfolioStatus.READY);

        assertThatThrownBy(() -> validator.validate(command(null, "가".repeat(3001), null)))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_JD_LENGTH);
    }

    @Test
    void jdText가_200_3000자_경계값이면_통과한다() {
        givenPortfolioStatus(PortfolioStatus.READY);

        assertThatCode(() -> validator.validate(command(null, "가".repeat(200), null))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(command(null, "가".repeat(3000), null))).doesNotThrowAnyException();
    }

    @Test
    void freeText가_10자_미만이면_INVALID_FREETEXT_LENGTH() {
        givenPortfolioStatus(PortfolioStatus.READY);

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(9))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_FREETEXT_LENGTH);
    }

    @Test
    void freeText가_300자_초과면_INVALID_FREETEXT_LENGTH() {
        givenPortfolioStatus(PortfolioStatus.READY);

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(301))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_FREETEXT_LENGTH);
    }

    @Test
    void freeText가_유효길이여도_연관성이_0_35미만이면_FREETEXT_NOT_RELEVANT() {
        givenPortfolioStatus(PortfolioStatus.READY);
        given(portfolioSimilarityCheckUseCase.checkSimilarity(any(), any())).willReturn(Optional.of(0.34));

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(20))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.FREETEXT_NOT_RELEVANT);
    }

    @Test
    void freeText_연관성_점수가_없으면_0점으로_취급해_FREETEXT_NOT_RELEVANT() {
        givenPortfolioStatus(PortfolioStatus.READY);
        given(portfolioSimilarityCheckUseCase.checkSimilarity(any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(command(null, null, "가".repeat(20))))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.FREETEXT_NOT_RELEVANT);
    }

    @Test
    void freeText가_유효길이이고_연관성이_0_35이상이면_통과한다() {
        givenPortfolioStatus(PortfolioStatus.READY);
        given(portfolioSimilarityCheckUseCase.checkSimilarity(any(), any())).willReturn(Optional.of(0.35));

        assertThatCode(() -> validator.validate(command(null, null, "가".repeat(20)))).doesNotThrowAnyException();
    }

    @Test
    void 포트폴리오_검증이_JD_검증보다_먼저_실행된다() {
        given(portfolioStatusUseCase.getStatus(userId, portfolioId))
                .willThrow(new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        assertThatThrownBy(() -> validator.validate(command(null, "가".repeat(1), null)))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }
}
