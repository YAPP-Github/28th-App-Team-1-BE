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
    void 이후에_다시_등장하지_않는_짧은_구간은_커서를_역행시키지_않고_소비할_문자가_없으면_생성하지_않는다() {
        // "네"는 인덱스 0에만 존재한다. 세 번째 세그먼트가 앞서 소비된 "네"와 같은 텍스트를 반환하면
        // (Whisper가 간투사를 중복/재인식하는 경우) cursor 이후 전방 탐색은 실패하고, 대본 끝까지도
        // 더 소비할 문자가 없으므로 이 세그먼트는 빈 세그먼트로 만들어지지 않고 버려진다.
        String fullText = "네 압니다";
        List<TranscriptSegment> sttSegments = List.of(
                new TranscriptSegment("네", 0.0f, 0.3f),
                new TranscriptSegment("압니다", 0.3f, 1.0f),
                new TranscriptSegment("네", 1.0f, 1.3f)
        );

        List<UtteranceSegment> result = ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, fullText, sttSegments, 0.0f);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).startIndex()).isZero();
        assertThat(result.get(1).startIndex()).isEqualTo(2);
    }

    @Test
    void 근사_배치는_이후의_정확한_매칭을_건너뛰지_않는다() {
        // "ZZZZ"는 원문에 없어 근사 배치되지만, 그 뒤 "반갑습니다"는 원문에 정확히 존재한다.
        // 근사 배치가 커서를 piece.length()만큼 무작정 전진시키면 "반갑습니다"의 실제 시작 위치를
        // 지나쳐버려 세 번째 세그먼트가 엉뚱한 문자를 가리키게 된다 — 이를 방지해야 한다.
        String fullText = "안녕하세요 반갑습니다";
        List<TranscriptSegment> sttSegments = List.of(
                new TranscriptSegment("안녕하세요", 0.0f, 1.0f),
                new TranscriptSegment("ZZZZ", 1.0f, 2.0f),
                new TranscriptSegment("반갑습니다", 2.0f, 3.0f)
        );

        List<UtteranceSegment> result = ScriptSegmentMapper.map(ScriptRole.INTERVIEWEE, fullText, sttSegments, 0.0f);

        assertThat(result).hasSize(3);
        UtteranceSegment third = result.get(2);
        assertThat(third.text()).isEqualTo("반갑습니다");
        assertThat(fullText.substring(third.startIndex(), third.endIndex())).isEqualTo("반갑습니다");
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
    void 연속으로_매칭에_실패해도_커서가_전진해_같은_구간을_중복_배치하지_않는다() {
        // 재현: 질문 재-STT 시 Whisper 세그먼트가 구두점 차이 등으로 원문과 연속으로 매칭되지 않으면,
        // 근사 배치(fallback)가 커서를 전진시키지 않을 경우 같은 위치에서 같은 길이의 슬라이스를 반복
        // 생성해 서로 다른 세그먼트가 완전히 동일한 텍스트로 중복 저장된다(#78 인터뷰어 대본 중복 버그).
        String fullText = "안녕하세요 반갑습니다 오늘 날씨가 좋네요";
        List<TranscriptSegment> sttSegments = List.of(
                new TranscriptSegment("안녕하세요", 0.0f, 1.0f),
                new TranscriptSegment("ZZZZ", 1.0f, 2.0f), // 원문에 없는 4글자 — 매칭 실패
                new TranscriptSegment("WWWW", 2.0f, 3.0f)  // 원문에 없는 4글자(다른 내용, 같은 길이) — 매칭 실패
        );

        List<UtteranceSegment> result = ScriptSegmentMapper.map(ScriptRole.INTERVIEWER, fullText, sttSegments, 0.0f);

        assertThat(result).hasSize(3);
        UtteranceSegment second = result.get(1);
        UtteranceSegment third = result.get(2);
        // 매칭에 실패한 두 세그먼트가 같은 길이라도, 커서가 전진했다면 서로 다른 위치를 가리켜야 한다.
        assertThat(third.startIndex()).isEqualTo(second.endIndex());
        assertThat(third.startIndex()).isGreaterThan(second.startIndex());
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
