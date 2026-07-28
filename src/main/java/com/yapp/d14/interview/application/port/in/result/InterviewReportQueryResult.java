package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ScriptRole;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewReportQueryResult(
        ReportStatus status,
        String headline,
        List<RedFlagNotice> redFlagNotices,
        Video video,
        List<Card> cards,
        GuestFeedbackSection guestFeedback
) {

    public record RedFlagNotice(
            RedFlagType type,
            String message
    ) {
    }

    public record Video(
            String url,
            boolean expired,
            LocalDateTime expiresAt
    ) {
    }

    // 카드 = 질문/답변 턴 하나. 같은 축(axis)에 속한 카드끼리는 axisOrder가 같고,
    // depthLevel로 그 축 안에서의 순서(꼬리질문 깊이)를 구분한다. 예: axisOrder=1인 카드가
    // depthLevel 1,2로 두 개면 화면에는 "질문 1-1"/"질문 1-2"로 표시된다.
    public record Card(
            int axisOrder,
            int depthLevel,
            String questionText,
            String transcript,
            List<HighlightSpan> highlightSpans,
            String resolutionNotice,
            List<RedFlagNotice> cardRedFlagNotices,
            String questionIntent,
            List<ScriptSegment> scriptSegments
    ) {
    }

    // 대본(질문/답변)을 문장 단위로 쪼갠 한 조각. 한 카드의 세그먼트는 startSec 오름차순(실제 발화 순서: 질문 → 답변)이다.
    // role은 이 문장이 질문/답변 중 무엇인지와, startIndex/endIndex가 어느 대본 문자열(questionText/transcript)
    // 기준 문자 오프셋인지를 함께 뜻한다. startSec/endSec는 합성 영상(=녹화) 타임라인 기준 발화 구간(초)이다.
    public record ScriptSegment(
            ScriptRole role,
            String text,
            int startIndex,
            int endIndex,
            float startSec,
            float endSec
    ) {
    }

    public record HighlightSpan(
            int startIndex,
            int endIndex,
            HighlightTone tone,
            String analysis,
            List<String> followUpQuestions
    ) {
    }

    public record GuestFeedbackSection(
            int participantCount,
            List<Guest> guests
    ) {
    }

    // 지인 한 명 = 항목 하나. 그 지인이 평가한 태도 항목들을 한데 묶는다.
    public record Guest(
            String alias,
            List<AttitudeRating> attitudeRatings
    ) {
    }

    public record AttitudeRating(
            String axis,
            int level,
            String comment
    ) {
    }
}
