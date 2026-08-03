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
        // Whisper 세그먼트 텍스트는 흔히 앞뒤 공백을 달고 오므로, 대본에서 찾을 실제 문장만 남긴다.
        List<TranscriptSegment> segments = new ArrayList<>();
        List<String> pieces = new ArrayList<>();
        for (TranscriptSegment segment : sttSegments) {
            String piece = segment.text() == null ? "" : segment.text().strip();
            if (!piece.isEmpty()) {
                segments.add(segment);
                pieces.add(piece);
            }
        }
        if (pieces.isEmpty()) {
            return List.of();
        }

        int[] starts = new int[pieces.size()];
        int[] ends = new int[pieces.size()];
        int cursor = 0;
        int i = 0;
        while (i < pieces.size()) {
            int found = fullText.indexOf(pieces.get(i), cursor);
            if (found >= 0) {
                starts[i] = found;
                ends[i] = found + pieces.get(i).length();
                cursor = ends[i];
                i++;
                continue;
            }
            // 정확 매칭 실패: 곧바로 커서 위치에 piece.length()만큼 근사 배치하면, 그 뒤에 실제로
            // 정확히 매칭되는 세그먼트가 있어도 커서가 그 시작 위치를 넘어가 건너뛰게 된다. 그래서 이후
            // 세그먼트 중 다시 정확히 매칭되는 "앵커"를 먼저 찾고, 실패한 구간들은 [cursor, 앵커 시작) 안에만
            // 나눠서 근사 배치한다 — 앵커가 없으면(끝까지 실패) 남은 텍스트 안에서만 배치한다.
            int anchor = i + 1;
            int anchorStart = -1;
            while (anchor < pieces.size()) {
                int f = fullText.indexOf(pieces.get(anchor), cursor);
                if (f >= 0) {
                    anchorStart = f;
                    break;
                }
                anchor++;
            }
            int limit = anchorStart >= 0 ? anchorStart : fullText.length();
            int failedEnd = anchorStart >= 0 ? anchor : pieces.size();
            int gapCursor = cursor;
            for (int k = i; k < failedEnd; k++) {
                int remaining = limit - gapCursor;
                if (remaining <= 0) {
                    starts[k] = ends[k] = limit;
                    continue;
                }
                int len = Math.min(pieces.get(k).length(), remaining);
                starts[k] = gapCursor;
                ends[k] = gapCursor + len;
                gapCursor = ends[k];
            }
            cursor = gapCursor;
            i = failedEnd;
        }

        List<UtteranceSegment> result = new ArrayList<>();
        for (int k = 0; k < pieces.size(); k++) {
            if (starts[k] == ends[k]) {
                // 더 소비할 문자가 없어 근사 배치조차 못한 경우(대본 끝에서 반복 실패) — 빈 세그먼트는 만들지 않는다.
                continue;
            }
            TranscriptSegment segment = segments.get(k);
            result.add(new UtteranceSegment(
                    role,
                    fullText.substring(starts[k], ends[k]),
                    starts[k],
                    ends[k],
                    clampNonNegative(offsetSec + segment.startSec()),
                    clampNonNegative(offsetSec + segment.endSec())
            ));
        }
        return result;
    }

    private static float clampNonNegative(float value) {
        return value < 0 ? 0f : value;
    }
}
