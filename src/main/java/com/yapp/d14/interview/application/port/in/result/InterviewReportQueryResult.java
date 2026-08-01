package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.HighlightReason;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ScriptRole;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewReportQueryResult(
        ReportStatus status,
        String headline,
        Video video,
        List<Card> cards,
        List<ScriptLine> script,
        GuestFeedbackSection guestFeedback
) {

    // 카드 단위 노출 레드플래그 안내(Card.cardRedFlagNotices)에서 쓰인다. 전체 보고서 단위 안내는 없다.
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
            String questionIntentTitle,
            String questionIntent,
            List<ScriptSegment> scriptSegments
    ) {
    }

    // 대본(면접관/면접자)을 문장 단위로 쪼갠 한 조각. 한 카드의 세그먼트는 startSec 오름차순(실제 발화 순서: 면접관 → 면접자)이다.
    // role은 이 문장이 면접관/면접자 발화 중 무엇인지와, startIndex/endIndex가 어느 대본 문자열(questionText/transcript)
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

    // 면접 전체 대본을 문장 단위로 이어붙인 타임라인 한 줄. 카드(채점 대상 턴) 유무와 무관하게
    // 첫 면접관 멘트 → 프로젝트 설명 답변 → … → 마지막 멘트까지 세션의 모든 발화를 startSec 오름차순으로 담는다.
    // 합성 영상(=녹화) 재생 위치(currentTime)로 이 한 배열만 훑어 현재 발화 중인 문장을 강조한다(#78).
    public record ScriptLine(
            ScriptRole role,
            String text,
            float startSec,
            float endSec
    ) {
    }

    public record HighlightSpan(
            int startIndex,
            int endIndex,
            HighlightTone tone,
            HighlightReason reason,
            String title,
            String analysis,
            List<String> followUpQuestions,
            // 이 하이라이트가 시작하는 지점의 합성 영상(=녹화) 타임라인 시각(초). "영상 보러가기" 버튼의 이동 지점.
            // 문장 발화 시각을 못 만들었으면(세그먼트 없음) null.
            Float startSec,
            // 아래 3개는 reason=OFF_INTENT(딴 답)일 때만 채운다(그 외 null). "질문 의도 ↔ 내 답변" 대비 UI 전용.
            // answerTopicTitle=내 답변 요지, questionIntentTitle/questionIntent=카드값 복사(프론트 편의).
            String answerTopicTitle,
            String questionIntentTitle,
            String questionIntent
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
