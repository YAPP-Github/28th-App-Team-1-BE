package com.yapp.d14.interview.domain;

import java.util.ArrayList;
import java.util.List;

// STT 발화 세그먼트(음성 내 상대 시각)를 대본 문자열에 정합시켜 문장 단위 발화 구간(UtteranceSegment)으로 만든다.
// - startIndex/endIndex: 세그먼트 텍스트를 fullText에서 앞에서부터(cursor 기준) 찾아 매긴 문자 오프셋.
//   fullText는 STT가 만든 전체 대본(세그먼트 텍스트의 연결)이라 앞에서부터의 탐색이 거의 항상 성공한다.
//   따라서 text = fullText.substring(startIndex, endIndex)로 인덱스·텍스트가 서로 정합한다.
// - startSec/endSec: 세그먼트 상대 시각에 offsetSec(답변=answerStartSec, 질문=questionStartSec)을 더한 영상 타임라인 시각.
public final class ScriptSegmentMapper {

    private ScriptSegmentMapper() {
    }

    public static List<UtteranceSegment> map(
            ScriptRole role, String fullText, List<TranscriptSegment> sttSegments, float offsetSec
    ) {
        if (fullText == null || fullText.isEmpty() || sttSegments == null || sttSegments.isEmpty()) {
            return List.of();
        }
        List<UtteranceSegment> result = new ArrayList<>();
        int cursor = 0;
        for (TranscriptSegment segment : sttSegments) {
            // Whisper 세그먼트 텍스트는 흔히 앞뒤 공백을 달고 오므로, 대본에서 찾을 실제 문장만 남긴다.
            String piece = segment.text() == null ? "" : segment.text().strip();
            if (piece.isEmpty()) {
                continue;
            }
            int start = locate(fullText, piece, cursor);
            int end;
            if (start < 0) {
                // 공백·정규화 차이 등으로 못 찾으면 커서 위치에 근사 배치한다(정확도보다 누락 방지 우선).
                start = Math.min(cursor, fullText.length());
                end = Math.min(start + piece.length(), fullText.length());
            } else {
                end = start + piece.length();
                cursor = end;
            }
            result.add(new UtteranceSegment(
                    role,
                    fullText.substring(start, end),
                    start,
                    end,
                    clampNonNegative(offsetSec + segment.startSec()),
                    clampNonNegative(offsetSec + segment.endSec())
            ));
        }
        return result;
    }

    private static int locate(String fullText, String piece, int cursor) {
        int found = fullText.indexOf(piece, cursor);
        return found >= 0 ? found : fullText.indexOf(piece);
    }

    private static float clampNonNegative(float value) {
        return value < 0 ? 0f : value;
    }
}
