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

    // getPlayback/getGuestStatus는 baseAt+30일 하드캡(isExpiredForGuest)으로 판정하므로, baseAt을 조절해 하드캡 이내/초과를 만든다.
    private InterviewVideo video(boolean composited, boolean guestExpired) {
        LocalDateTime baseAt = guestExpired ? NOW.minusDays(31) : NOW.minusDays(1);
        return InterviewVideo.of(1L, SESSION_ID, baseAt, baseAt.plusHours(24), false, true, composited);
    }

    @Test
    void 합성이_끝났고_30일_하드캡_이내면_합성본_presigned_URL과_상태를_함께_반환한다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video(true, false)));
        given(interviewSessionOwnerQueryUseCase.getOwnerUserId(SESSION_ID)).willReturn(OWNER_ID);
        given(interviewVideoStorage.presignComposite(OWNER_ID, SESSION_ID)).willReturn("https://s3/final.mp4");

        var result = service.getPlayback(SESSION_ID);
        assertThat(result.playbackUrl()).isEqualTo("https://s3/final.mp4");
        assertThat(result.expired()).isFalse();
    }

    @Test
    void 합성_전이면_URL_없이_null을_반환한다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video(false, false)));

        assertThat(service.getPlayback(SESSION_ID).playbackUrl()).isNull();
        verify(interviewVideoStorage, never()).presignComposite(any(), any());
    }

    @Test
    void 합성됐어도_30일_하드캡을_넘겼으면_null을_반환한다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video(true, true)));

        var result = service.getPlayback(SESSION_ID);
        assertThat(result.playbackUrl()).isNull();
        assertThat(result.expired()).isTrue();
        verify(interviewVideoStorage, never()).presignComposite(any(), any());
    }

    @Test
    void 소유자_단계형_만료_시각은_지났어도_30일_하드캡_이내면_지인용_재생_URL을_준다() {
        LocalDateTime baseAt = NOW.minusDays(10);
        InterviewVideo ownerExpiredButGuestOpen = InterviewVideo.of(
                1L, SESSION_ID, baseAt, baseAt.plusHours(48), false, true, true
        );
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(ownerExpiredButGuestOpen));
        given(interviewSessionOwnerQueryUseCase.getOwnerUserId(SESSION_ID)).willReturn(OWNER_ID);
        given(interviewVideoStorage.presignComposite(OWNER_ID, SESSION_ID)).willReturn("https://s3/final.mp4");

        var result = service.getPlayback(SESSION_ID);

        assertThat(result.expired()).isFalse();
        assertThat(result.playbackUrl()).isEqualTo("https://s3/final.mp4");
    }

    @Test
    void 영상_레코드가_없으면_예외를_던진다() {
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPlayback(SESSION_ID)).isInstanceOf(InterviewException.class);
    }

    @Test
    void getOwnerStatus는_단계형_만료_시각_기준으로_판정한다() {
        LocalDateTime baseAt = NOW.minusDays(10);
        InterviewVideo video = InterviewVideo.of(1L, SESSION_ID, baseAt, baseAt.plusHours(48), false, true, true);
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video));

        var result = service.getOwnerStatus(SESSION_ID);

        assertThat(result.expired()).isTrue();
        assertThat(result.expiresAt()).isEqualTo(baseAt.plusHours(48));
    }

    @Test
    void getGuestStatus는_baseAt_30일_하드캡_기준으로_판정하고_하드캡_시각을_반환한다() {
        LocalDateTime baseAt = NOW.minusDays(10);
        InterviewVideo video = InterviewVideo.of(1L, SESSION_ID, baseAt, baseAt.plusHours(48), false, true, true);
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(video));

        var result = service.getGuestStatus(SESSION_ID);

        assertThat(result.expired()).isFalse();
        assertThat(result.expiresAt()).isEqualTo(baseAt.plusDays(30));
    }
}
