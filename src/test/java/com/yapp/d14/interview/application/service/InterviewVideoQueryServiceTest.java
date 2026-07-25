package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnerQueryUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewVideoQueryServiceTest {

    private static final Long SESSION_ID = 100L;
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private InterviewVideoRepository interviewVideoRepository;
    @Mock
    private InterviewVideoStorage interviewVideoStorage;
    @Mock
    private InterviewSessionOwnerQueryUseCase interviewSessionOwnerQueryUseCase;

    @InjectMocks
    private InterviewVideoQueryService service;

    private InterviewVideo video(boolean composited, boolean expiredInFuture) {
        LocalDateTime expiresAt = expiredInFuture ? NOW.plusDays(1) : NOW.minusDays(1);
        return InterviewVideo.of(1L, SESSION_ID, NOW, expiresAt, false, true, composited);
    }

    @Test
    void 합성이_끝났고_만료_전이면_합성본_presigned_URL을_반환한다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video(true, true)));
        given(interviewSessionOwnerQueryUseCase.getOwnerUserId(SESSION_ID)).willReturn(OWNER_ID);
        given(interviewVideoStorage.presignComposite(OWNER_ID, SESSION_ID)).willReturn("https://s3/final.mp4");

        assertThat(service.getPlaybackUrl(SESSION_ID)).isEqualTo("https://s3/final.mp4");
    }

    @Test
    void 합성_전이면_URL_없이_null을_반환한다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video(false, true)));

        assertThat(service.getPlaybackUrl(SESSION_ID)).isNull();
        verify(interviewVideoStorage, never()).presignComposite(any(), any());
    }

    @Test
    void 합성됐어도_만료됐으면_null을_반환한다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video(true, false)));

        assertThat(service.getPlaybackUrl(SESSION_ID)).isNull();
        verify(interviewVideoStorage, never()).presignComposite(any(), any());
    }

    @Test
    void 영상_레코드가_없으면_예외를_던진다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPlaybackUrl(SESSION_ID)).isInstanceOf(InterviewException.class);
    }
}
