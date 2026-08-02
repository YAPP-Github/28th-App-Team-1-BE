package com.yapp.d14.interview.adapter.in.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class M4aAudioFormatValidatorTest {

    @Test
    void 오디오_트랙만_있는_mp4는_통과한다() {
        assertThat(M4aAudioFormatValidator.isM4aAudio(Mp4Fixtures.validM4aAudio())).isTrue();
    }

    @Test
    void 비디오_트랙이_있으면_거부한다() {
        assertThat(M4aAudioFormatValidator.isM4aAudio(Mp4Fixtures.videoTrack())).isFalse();
    }

    @Test
    void 오디오_트랙이지만_AAC가_아니면_거부한다() {
        assertThat(M4aAudioFormatValidator.isM4aAudio(Mp4Fixtures.nonAacAudioTrack())).isFalse();
    }

    @Test
    void MP4_컨테이너가_아니면_거부한다() {
        assertThat(M4aAudioFormatValidator.isM4aAudio(Mp4Fixtures.notMp4())).isFalse();
    }

    @Test
    void 바이트가_null이거나_너무_짧으면_거부한다() {
        assertThat(M4aAudioFormatValidator.isM4aAudio(null)).isFalse();
        assertThat(M4aAudioFormatValidator.isM4aAudio(new byte[]{1, 2, 3})).isFalse();
    }
}
