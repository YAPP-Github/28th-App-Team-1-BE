package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InterviewSessionInProgressCheckServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @InjectMocks
    private InterviewSessionInProgressCheckService service;

    @Test
    void 해당_포트폴리오를_사용하는_IN_PROGRESS_세션이_있으면_true를_반환한다() {
        UUID portfolioId = UUID.randomUUID();
        given(interviewSessionRepository.existsByPortfolioIdAndStatus(portfolioId, InterviewSessionStatus.IN_PROGRESS))
                .willReturn(true);

        assertThat(service.existsInProgress(portfolioId)).isTrue();
    }

    @Test
    void 해당_포트폴리오를_사용하는_IN_PROGRESS_세션이_없으면_false를_반환한다() {
        UUID portfolioId = UUID.randomUUID();
        given(interviewSessionRepository.existsByPortfolioIdAndStatus(portfolioId, InterviewSessionStatus.IN_PROGRESS))
                .willReturn(false);

        assertThat(service.existsInProgress(portfolioId)).isFalse();
    }
}
