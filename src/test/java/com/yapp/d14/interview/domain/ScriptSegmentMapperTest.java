package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptSegmentMapperTest {

    @Test
    void 세그먼트를_대본_문자_인덱스에_정합시키고_오프셋을_더한다() {
        String fullText = "안녕하세요. 반갑습니다.";
        List<TranscriptSegment> sttSegments = List.of(
                new TranscriptSegment("안녕하세요.", 0.0f, 1.2f),
                // Whisper 세그먼트는 앞에 공백을 달고 오는 경우가 많다 — strip 후 대본에서 찾는다.
                new TranscriptSegment(" 반갑습니다.", 1.2f, 2.4f)
        );

        List<UtteranceSegment> result = ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, fullText, sttSegments, 10.0f);

        assertThat(result).hasSize(2);

        UtteranceSegment first = result.get(0);
        assertThat(first.role()).isEqualTo(ScriptRole.INTERVIEWEE);
        assertThat(first.text()).isEqualTo("안녕하세요.");
        assertThat(first.startIndex()).isZero();
        assertThat(first.endIndex()).isEqualTo(6);
        assertThat(fullText.substring(first.startIndex(), first.endIndex())).isEqualTo(first.text());
        // 상대 0.0/1.2에 오프셋 10.0을 더한 값
        assertThat(first.startSec()).isEqualTo(10.0f);
        assertThat(first.endSec()).isEqualTo(11.2f);

        UtteranceSegment second = result.get(1);
        assertThat(second.text()).isEqualTo("반갑습니다.");
        assertThat(second.startIndex()).isEqualTo(7);
        assertThat(second.endIndex()).isEqualTo(13);
        assertThat(fullText.substring(second.startIndex(), second.endIndex())).isEqualTo(second.text());
        assertThat(second.startSec()).isEqualTo(11.2f);
        assertThat(second.endSec()).isEqualTo(12.4f);
    }

    @Test
    void 같은_문장이_반복돼도_커서_이후에서_찾아_순서를_유지한다() {
        String fullText = "네. 네.";
        List<TranscriptSegment> sttSegments = List.of(
                new TranscriptSegment("네.", 0.0f, 0.5f),
                new TranscriptSegment(" 네.", 0.5f, 1.0f)
        );

        List<UtteranceSegment> result = ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, fullText, sttSegments, 0.0f);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).startIndex()).isZero();
        assertThat(result.get(1).startIndex()).isEqualTo(3);
    }

    @Test
    void 공백뿐인_세그먼트는_건너뛴다() {
        String fullText = "반갑습니다.";
        List<TranscriptSegment> sttSegments = List.of(
                new TranscriptSegment("   ", 0.0f, 0.3f),
                new TranscriptSegment("반갑습니다.", 0.3f, 1.5f)
        );

        List<UtteranceSegment> result = ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, fullText, sttSegments, 0.0f);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).text()).isEqualTo("반갑습니다.");
    }

    @Test
    void 세그먼트가_없거나_대본이_비면_빈_리스트다() {
        assertThat(ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, "대본", List.of(), 0.0f)).isEmpty();
        assertThat(ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, "", List.of(new TranscriptSegment("x", 0f, 1f)), 0.0f)).isEmpty();
        assertThat(ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, null, List.of(new TranscriptSegment("x", 0f, 1f)), 0.0f)).isEmpty();
    }

    @Test
    void 음수_시각은_0으로_보정한다() {
        // 오프셋이 없고 세그먼트 상대 시각이 음수로 들어오는 비정상 입력 방어
        List<UtteranceSegment> result = ScriptSegmentMapper.map(
                ScriptRole.INTERVIEWER, "질문입니다.", List.of(new TranscriptSegment("질문입니다.", -0.5f, 1.0f)), 0.0f);

        assertThat(result.get(0).startSec()).isEqualTo(0.0f);
        assertThat(result.get(0).endSec()).isEqualTo(1.0f);
    }
}
