package com.yapp.d14.interview.adapter.out.integration.media;

import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor.AudioTrack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ffmpeg 바이너리 없이 필터 문자열 조합만 검증하는 순수 단위 테스트(CI 포함 실행).
 * 실제 합성 검증은 별도 스모크 테스트(FfmpegInterviewVideoCompositorAdapterSmokeTest, compositeSmokeTest)에서 한다.
 */
class FfmpegInterviewVideoCompositorAdapterTest {

    // S3 의존성은 buildAudioFilter에서 쓰지 않으므로 null로 둔다.
    private final FfmpegInterviewVideoCompositorAdapter adapter =
            new FfmpegInterviewVideoCompositorAdapter(null, null);

    @Test
    void 트랙이_하나면_amix_없이_지연_결과를_바로_aout으로_쓴다() {
        String filter = adapter.buildAudioFilter(List.of(new AudioTrack("k", 1.0f)));

        assertThat(filter).isEqualTo("[1:a]adelay=1000:all=1[aout]");
    }

    @Test
    void 트랙이_여럿이면_각_입력을_지연시킨_뒤_amix로_믹싱한다() {
        String filter = adapter.buildAudioFilter(List.of(
                new AudioTrack("q", 1.0f),
                new AudioTrack("a", 4.0f)
        ));

        assertThat(filter).isEqualTo(
                "[1:a]adelay=1000:all=1[a0];[2:a]adelay=4000:all=1[a1];[a0][a1]amix=inputs=2:normalize=0[aout]");
    }

    @Test
    void 트랙_세_개도_입력_인덱스_1부터_순서대로_믹싱한다() {
        String filter = adapter.buildAudioFilter(List.of(
                new AudioTrack("q1", 1.0f),
                new AudioTrack("a1", 2.5f),
                new AudioTrack("q2", 4.0f)
        ));

        assertThat(filter).isEqualTo(
                "[1:a]adelay=1000:all=1[a0];[2:a]adelay=2500:all=1[a1];[3:a]adelay=4000:all=1[a2];"
                        + "[a0][a1][a2]amix=inputs=3:normalize=0[aout]");
    }

    @Test
    void 시작초는_ms로_반올림되고_0초는_지연_0이다() {
        assertThat(adapter.buildAudioFilter(List.of(new AudioTrack("k", 0.0f))))
                .isEqualTo("[1:a]adelay=0:all=1[aout]");
        assertThat(adapter.buildAudioFilter(List.of(new AudioTrack("k", 1.5f))))
                .isEqualTo("[1:a]adelay=1500:all=1[aout]");
    }
}
