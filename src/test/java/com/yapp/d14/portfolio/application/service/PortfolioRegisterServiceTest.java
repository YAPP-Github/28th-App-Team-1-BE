package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.PortfolioProcessUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.out.PdfMetadataReader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioRegisterServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PdfMetadataReader pdfMetadataReader;

    @Mock
    private PortfolioProcessUseCase portfolioProcessUseCase;

    @InjectMocks
    private PortfolioRegisterService portfolioRegisterService;

    private UUID userId;
    private PortfolioRegisterCommand command;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        command = new PortfolioRegisterCommand(userId, "fake-pdf-bytes".getBytes(), "resume.pdf", 14, 5, "application/pdf");
    }

    @Test
    void 정상_등록되면_PROCESSING_상태로_반환하고_비동기_처리를_요청한다() {
        given(pdfMetadataReader.countPages(command.fileContent())).willReturn(5);
        given(portfolioRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        PortfolioRegisterResult result = portfolioRegisterService.register(command);

        assertThat(result.status()).isEqualTo(PortfolioStatus.PROCESSING);
        verify(portfolioProcessUseCase).process(userId, result.portfolioId(), command.fileContent());
        verify(portfolioRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void 비동기_처리_큐가_가득_차면_FAILED_SYSTEM으로_전환해서_반환한다() {
        given(pdfMetadataReader.countPages(command.fileContent())).willReturn(5);
        given(portfolioRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RejectedExecutionException("큐 가득 참"))
                .when(portfolioProcessUseCase)
                .process(any(), any(), any());

        PortfolioRegisterResult result = portfolioRegisterService.register(command);

        assertThat(result.status()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
        ArgumentCaptor<com.yapp.d14.portfolio.domain.Portfolio> captor =
                ArgumentCaptor.forClass(com.yapp.d14.portfolio.domain.Portfolio.class);
        verify(portfolioRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
    }
}
