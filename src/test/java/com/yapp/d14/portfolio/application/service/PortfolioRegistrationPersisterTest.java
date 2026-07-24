package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioRegistrationPersisterTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private PortfolioRegistrationPersister portfolioRegistrationPersister;

    private UUID userId;
    private PortfolioRegisterCommand command;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        command = new PortfolioRegisterCommand(userId, "fake-pdf-bytes".getBytes(), "resume.pdf", 14, 5, "application/pdf");
    }

    @Test
    void 저장_전에_유저_단위_등록_락을_먼저_획득한다() {
        given(portfolioRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        portfolioRegistrationPersister.persist(command, 5);

        InOrder order = inOrder(portfolioRepository);
        order.verify(portfolioRepository).acquireRegistrationLock(userId);
        order.verify(portfolioRepository).existsActiveByUserId(userId);
        order.verify(portfolioRepository).save(any());
    }

    @Test
    void 활성_포트폴리오가_있으면_PORTFOLIO_ALREADY_EXISTS() {
        given(portfolioRepository.existsActiveByUserId(userId)).willReturn(true);

        assertThatThrownBy(() -> portfolioRegistrationPersister.persist(command, 5))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);

        verify(portfolioRepository, never()).save(any());
    }

    @Test
    void 재업로드이고_이번달_재업로드_이력이_있으면_REPLACEMENT_LIMIT_EXCEEDED() {
        given(portfolioRepository.existsAnyByUserId(userId)).willReturn(true);
        given(portfolioRepository.existsReplacementSince(any(), any())).willReturn(true);

        assertThatThrownBy(() -> portfolioRegistrationPersister.persist(command, 5))
                .isInstanceOf(PortfolioException.class)
                .extracting(e -> ((PortfolioException) e).getErrorCode())
                .isEqualTo(PortfolioErrorCode.REPLACEMENT_LIMIT_EXCEEDED);

        verify(portfolioRepository, never()).save(any());
    }

    @Test
    void 재업로드이고_이번달_재업로드_이력이_없으면_replacement_true로_등록된다() {
        given(portfolioRepository.existsAnyByUserId(userId)).willReturn(true);
        given(portfolioRepository.existsReplacementSince(any(), any())).willReturn(false);
        given(portfolioRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        portfolioRegistrationPersister.persist(command, 5);

        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).save(captor.capture());
        assertThat(captor.getValue().isReplacement()).isTrue();
    }

    @Test
    void 최초_업로드면_replacement_false로_등록된다() {
        given(portfolioRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        portfolioRegistrationPersister.persist(command, 5);

        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).save(captor.capture());
        assertThat(captor.getValue().isReplacement()).isFalse();
    }
}
