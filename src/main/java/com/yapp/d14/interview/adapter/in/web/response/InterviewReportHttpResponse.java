package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewReportQueryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewReportHttpResponse(
        @Schema(description = "채점 파이프라인 진행 상태 — GENERATING(채점 중) / READY(생성 완료) / INSUFFICIENT_ANALYSIS(분석 부족) / FAILED(생성 실패). " +
                "심각 레드플래그 여부는 이 필드가 아니라 redFlagNotices로 판단한다(READY이면서 redFlagNotices가 있으면 심각 레드플래그)")
        String status,

        @Schema(description = "한 줄 요약(헤드라인). GENERATING일 때는 null")
        String headline,

        @Schema(description = "헤드라인 아래 표기되는 레드플래그 안내 줄(노출 3종만, 최대 2줄)")
        List<RedFlagNotice> redFlagNotices,

        @Schema(description = "면접 영상 플레이어 메타. GENERATING일 때는 null")
        Video video,

        @Schema(description = "질문/답변 턴 하나당 카드 하나. GENERATING일 때는 null")
        List<Card> cards,

        @Schema(description = "지인 피드백 섹션. 제출한 지인이 없으면 null")
        GuestFeedbackSection guestFeedback
) {

    public static InterviewReportHttpResponse from(InterviewReportQueryResult result) {
        return new InterviewReportHttpResponse(
                result.status().name(),
                result.headline(),
                result.redFlagNotices() == null ? null : result.redFlagNotices().stream().map(RedFlagNotice::from).toList(),
                Video.from(result.video()),
                result.cards() == null ? null : result.cards().stream().map(Card::from).toList(),
                GuestFeedbackSection.from(result.guestFeedback())
        );
    }

    public record RedFlagNotice(
            @Schema(description = "레드플래그 유형 — CONTRADICTION(모순) / FABRICATION(지어냄) / PERFECT_NARRATIVE(무결점 서사)")
            String type,

            @Schema(description = "중립 안내 문구")
            String message
    ) {

        private static RedFlagNotice from(InterviewReportQueryResult.RedFlagNotice notice) {
            return new RedFlagNotice(notice.type().name(), notice.message());
        }
    }

    public record Video(
            @Schema(description = "영상 재생 URL. 만료됐으면 null")
            String url,

            @Schema(description = "영상 만료 여부")
            boolean expired,

            @Schema(description = "영상 삭제 예정 시각")
            LocalDateTime expiresAt
    ) {

        private static Video from(InterviewReportQueryResult.Video video) {
            if (video == null) {
                return null;
            }
            return new Video(video.url(), video.expired(), video.expiresAt());
        }
    }

    public record Card(
            @Schema(description = "같은 항목(축)에 속한 카드끼리 공유하는 순번(1부터) — 화면 표시용 \"질문 {axisOrder}-{depthLevel}\"의 앞자리")
            int axisOrder,

            @Schema(description = "같은 항목(축) 안에서 이 카드(턴)의 순서(1부터) — 화면 표시용 \"질문 {axisOrder}-{depthLevel}\"의 뒷자리")
            int depthLevel,

            @Schema(description = "질문 텍스트")
            String questionText,

            @Schema(description = "답변 대본(STT)")
            String transcript,

            @Schema(description = "대본(transcript) 위 하이라이트 구간. 하이라이트마다 그 근거로 단 행동형 키워드를 갖는다. 해상도 낮음 카드는 빈 배열")
            List<HighlightSpan> highlightSpans,

            @Schema(description = "해상도 낮음 안내 문구. 정상 카드는 null")
            String resolutionNotice,

            @Schema(description = "이 카드에 걸린 레드플래그 안내 줄")
            List<RedFlagNotice> cardRedFlagNotices,

            @Schema(description = "질문 분석(질문 의도 설명, probe_text 번역)")
            String questionIntent
    ) {

        private static Card from(InterviewReportQueryResult.Card card) {
            return new Card(
                    card.axisOrder(),
                    card.depthLevel(),
                    card.questionText(),
                    card.transcript(),
                    card.highlightSpans() == null ? null : card.highlightSpans().stream().map(HighlightSpan::from).toList(),
                    card.resolutionNotice(),
                    card.cardRedFlagNotices() == null ? null : card.cardRedFlagNotices().stream().map(RedFlagNotice::from).toList(),
                    card.questionIntent()
            );
        }
    }

    public record HighlightSpan(
            @Schema(description = "대본(transcript) 문자열 기준 하이라이트 시작 인덱스(0부터, 포함)")
            int startIndex,

            @Schema(description = "대본(transcript) 문자열 기준 하이라이트 종료 인덱스(미포함)")
            int endIndex,

            @Schema(description = "하이라이트 톤 — GOOD(잘함) / IMPROVE(개선)")
            String tone,

            @Schema(description = "이 하이라이트 구간을 근거로 단 행동형 키워드(하이라이트당 최대 3개). 탭하면 열리는 상세 시트 내용")
            List<ActionKeyword> actionKeywords
    ) {

        private static HighlightSpan from(InterviewReportQueryResult.HighlightSpan span) {
            return new HighlightSpan(
                    span.startIndex(),
                    span.endIndex(),
                    span.tone().name(),
                    span.actionKeywords() == null ? null : span.actionKeywords().stream().map(ActionKeyword::from).toList()
            );
        }
    }

    public record ActionKeyword(
            @Schema(description = "행동형 키워드")
            String keyword,

            @Schema(description = "그 방향으로 가야 하는 이유와 다음 면접에서 어떻게 적용하면 되는지를 담은 방향성 제안")
            String suggestion,

            @Schema(description = "'이렇게 바꿔 말해보세요' 고쳐 쓴 문장. 재료가 없으면 null")
            String rewrittenText
    ) {

        private static ActionKeyword from(InterviewReportQueryResult.ActionKeyword keyword) {
            return new ActionKeyword(keyword.keyword(), keyword.suggestion(), keyword.rewrittenText());
        }
    }

    public record GuestFeedbackSection(
            @Schema(description = "참여 지인 수(참고치)")
            int participantCount,

            @Schema(description = "지인별 피드백. 지인 한 명당 원소 하나 — 그 지인이 평가한 태도 항목들을 담는다")
            List<Guest> guests
    ) {

        private static GuestFeedbackSection from(InterviewReportQueryResult.GuestFeedbackSection section) {
            if (section == null) {
                return null;
            }
            return new GuestFeedbackSection(
                    section.participantCount(),
                    section.guests() == null ? null : section.guests().stream().map(Guest::from).toList()
            );
        }
    }

    public record Guest(
            @Schema(description = "지인 별칭")
            String alias,

            @Schema(description = "이 지인이 평가한 태도 항목별 척도·코멘트")
            List<AttitudeRating> attitudeRatings
    ) {

        private static Guest from(InterviewReportQueryResult.Guest guest) {
            return new Guest(
                    guest.alias(),
                    guest.attitudeRatings() == null ? null : guest.attitudeRatings().stream().map(AttitudeRating::from).toList()
            );
        }
    }

    public record AttitudeRating(
            @Schema(description = "태도 항목 코드 — GAZE(시선) / EXPRESSION(표정) / POSTURE(자세) / GESTURE(손동작) / VOICE(목소리)")
            String axis,

            @Schema(description = "4단계 척도 값(1~4).")
            int level,

            @Schema(description = "지인이 남긴 코멘트. 없으면 null")
            String comment
    ) {

        private static AttitudeRating from(InterviewReportQueryResult.AttitudeRating rating) {
            return new AttitudeRating(rating.axis(), rating.level(), rating.comment());
        }
    }
}
