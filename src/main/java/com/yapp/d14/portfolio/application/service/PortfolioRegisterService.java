package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioProcessUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterUseCase;
import com.yapp.d14.portfolio.application.port.out.PdfMetadataReader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
class PortfolioRegisterService implements PortfolioRegisterUseCase {

    private final PdfMetadataReader pdfMetadataReader;
    private final PortfolioRegistrationPersister portfolioRegistrationPersister;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioProcessUseCase portfolioProcessUseCase;

    @Override
    public PortfolioRegisterResult register(PortfolioRegisterCommand command) {
        int pageCount = pdfMetadataReader.countPages(command.fileContent());
        if (pageCount > PortfolioRegisterCommand.MAX_PAGE_COUNT) {
            throw new PortfolioException(PortfolioErrorCode.PAGE_COUNT_EXCEEDED);
        }

        Portfolio saved = portfolioRegistrationPersister.persist(command, pageCount);

        try {
            portfolioProcessUseCase.process(command.userId(), saved.getId(), command.fileContent());
        } catch (RejectedExecutionException e) {
            log.error("[PORTFOLIO REGISTER] 비동기 처리 큐가 가득 찼어요: portfolioId={}", saved.getId(), e);
            saved.failSystem("현재 요청이 많아 처리가 지연되고 있어요. 잠시 후 다시 시도해 주세요.");
            saved = portfolioRepository.save(saved);
        }

        return new PortfolioRegisterResult(saved.getId(), saved.getStatus(), saved.getMessage());
    }
}
