package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoExpiryResult;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.exception.InterviewException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewVideoExpiryQueryServiceTest {

    private static final Long SESSION_ID = 100L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    @Mock
    private InterviewVideoRepository interviewVideoRepository;

    @InjectMocks
    private InterviewVideoExpiryQueryService service;

    private InterviewVideo video(LocalDateTime expiresAt) {
        return InterviewVideo.of(1L, SESSION_ID, NOW, expiresAt, false, true, true, null, null);
    }

    @Test
    void 소유권을_확인하고_만료까지_남은_초를_반환한다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(video(LocalDateTime.now().plusDays(10))));

        InterviewVideoExpiryResult result = service.getExpiry(USER_ID, SESSION_ID);

        verify(interviewSessionOwnershipCheckUseCase).requireOwned(USER_ID, SESSION_ID);
        assertThat(result.expired()).isFalse();
        // 10일 = 864,000초 근방(테스트 실행 시간차 감안 오차 허용).
        assertThat(result.expiresInSeconds()).isBetween(863_990L, 864_000L);
    }

    @Test
    void 이미_만료됐으면_0을_반환하고_expired는_true다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(video(LocalDateTime.now().minusDays(1))));

        InterviewVideoExpiryResult result = service.getExpiry(USER_ID, SESSION_ID);

        assertThat(result.expiresInSeconds()).isZero();
        assertThat(result.expired()).isTrue();
    }

    @Test
    void 영상_레코드가_없으면_예외를_던진다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExpiry(USER_ID, SESSION_ID)).isInstanceOf(InterviewException.class);
    }
}
