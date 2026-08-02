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
                // 이 경우에도 커서를 전진시켜야 한다 — 그러지 않으면 다음 세그먼트들이 매칭에 계속 실패할 때
                // (예: Whisper 세그먼트가 원문과 구두점이 달라 연속으로 못 찾는 경우) 같은 위치에서 근사 배치가
                // 반복돼 동일한 텍스트가 중복 저장된다(#78 인터뷰어 대본 중복 버그).
                start = Math.min(cursor, fullText.length());
                end = Math.min(start + piece.length(), fullText.length());
                cursor = end;
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
        if (found >= 0) {
            return found;
        }
        int fallback = fullText.indexOf(piece);
        // 폴백 검색 결과가 cursor보다 앞이면 채택하지 않는다 — 반복되는 짧은 간투사("네.", "음." 등)에서
        // cursor를 뒤로 되돌려 이후 세그먼트의 인덱스가 역행·중복되는 것을 막는다.
        return fallback >= cursor ? fallback : -1;
    }

    private static float clampNonNegative(float value) {
        return value < 0 ? 0f : value;
    }
}
