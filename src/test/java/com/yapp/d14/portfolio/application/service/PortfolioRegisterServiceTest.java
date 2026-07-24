package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.PortfolioProcessUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.out.PdfMetadataReader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioRegisterServiceTest {

    @Mock
    private PdfMetadataReader pdfMetadataReader;

    @Mock
    private PortfolioRegistrationPersister portfolioRegistrationPersister;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioProcessUseCase portfolioProcessUseCase;

    @InjectMocks
    private PortfolioRegisterService portfolioRegisterService;

    private UUID userId;
    private PortfolioRegisterCommand command;
    private Portfolio persisted;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        command = new PortfolioRegisterCommand(userId, "fake-pdf-bytes".getBytes(), "resume.pdf", 14, 5, "application/pdf");
        persisted = Portfolio.create(
                UUID.randomUUID(), userId, "resume.pdf", command.fileContent().length, 5,
                "users/%s/portfolios/%s/test.pdf".formatted(userId, UUID.randomUUID()), false
        );
    }

    @Test
    void 정상_등록되면_PROCESSING_상태로_반환하고_비동기_처리를_요청한다() {
        given(pdfMetadataReader.countPages(command.fileContent())).willReturn(5);
        given(portfolioRegistrationPersister.persist(command, 5)).willReturn(persisted);

        PortfolioRegisterResult result = portfolioRegisterService.register(command);

        assertThat(result.status()).isEqualTo(PortfolioStatus.PROCESSING);
        assertThat(result.portfolioId()).isEqualTo(persisted.getId());
        verify(portfolioProcessUseCase).process(userId, persisted.getId(), command.fileContent());
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    void 페이지수가_초과되면_등록을_시도하지_않는다() {
        given(pdfMetadataReader.countPages(command.fileContent())).willReturn(31);

        assertThatThrownBy(() -> portfolioRegisterService.register(command))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PAGE_COUNT_EXCEEDED);

        verify(portfolioRegistrationPersister, never()).persist(any(), anyInt());
    }

    @Test
    void 등록_제약을_위반하면_예외가_그대로_전파된다() {
        given(pdfMetadataReader.countPages(command.fileContent())).willReturn(5);
        given(portfolioRegistrationPersister.persist(command, 5))
                .willThrow(new PortfolioException(PortfolioErrorCode.REPLACEMENT_LIMIT_EXCEEDED));

        assertThatThrownBy(() -> portfolioRegisterService.register(command))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.REPLACEMENT_LIMIT_EXCEEDED);

        verify(portfolioProcessUseCase, never()).process(any(), any(), any());
    }

    @Test
    void 비동기_처리_큐가_가득_차면_FAILED_SYSTEM으로_전환해서_반환한다() {
        given(pdfMetadataReader.countPages(command.fileContent())).willReturn(5);
        given(portfolioRegistrationPersister.persist(command, 5)).willReturn(persisted);
        given(portfolioRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RejectedExecutionException("큐 가득 참"))
                .when(portfolioProcessUseCase)
                .process(any(), any(), any());

        PortfolioRegisterResult result = portfolioRegisterService.register(command);

        assertThat(result.status()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
    }
}
