package com.yapp.d14.interview.application.service;

import com.yapp.d14.feedback.application.port.in.GuestFeedbackReportQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackReportView;
import com.yapp.d14.interview.application.port.in.InterviewReportQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportQueryResult;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.AxisEvaluationRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.RedFlagRepository;
import com.yapp.d14.interview.application.port.out.ReportCardRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.application.port.out.UtteranceSegmentRepository;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.InterviewEndType;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.ResolutionLowReason;
import com.yapp.d14.interview.domain.HighlightReason;
import com.yapp.d14.interview.domain.ScriptRole;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.UtteranceSegment;
import com.yapp.d14.interview.domain.WrapUpMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewReportQueryService implements InterviewReportQueryUseCase {

    // 노출 레드플래그 3종의 사용자용 중립 안내 문구.
    private static final Map<RedFlagType, String> RED_FLAG_NOTICE = Map.of(
            // 지어냄: 경험을 구체적으로 캐물었을 때 실제로 한 일이라 보기 어려운 정황(경험의 진위).
            RedFlagType.FABRICATION, "경험을 구체적으로 파고드는 질문에서, 실제로 한 일이라고 보기 어려운 지점이 있었어요.",
            // 일관성 붕괴: 면접 앞부분과 뒷부분의 진술이 서로 어긋남(진술 간 일관성).
            RedFlagType.CONTRADICTION, "면접 앞부분과 뒷부분의 답변이 서로 어긋나는 지점이 있었어요.",
            // 무결점 서사: 약점·비용을 묻는 탐침에도 어려움을 인정하지 않음.
            RedFlagType.PERFECT_NARRATIVE, "약점·비용을 묻는 질문에도 어려움을 인정한 부분이 거의 없었어요."
    );

    // 해상도 낮음(LOW) 축의 카드에 붙는 사유별 안내 문구.
    private static final Map<ResolutionLowReason, String> RESOLUTION_NOTICE = Map.of(
            ResolutionLowReason.FEW_TURNS, "답변이 충분하지 않아 이 항목은 능력 판단을 보류했어요.",
            ResolutionLowReason.SHALLOW_ANSWER, "답변이 짧고 얕아 이 항목은 능력 판단을 보류했어요.",
            ResolutionLowReason.OFF_TOPIC, "질문과 다른 답변이 있어 이 항목은 능력 판단을 보류했어요."
    );

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final InterviewSessionRepository interviewSessionRepository;
    private final ReportRepository reportRepository;
    private final ReportCardRepository reportCardRepository;
    private final RedFlagRepository redFlagRepository;
    private final AxisEvaluationRepository axisEvaluationRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final InterviewVideoRepository interviewVideoRepository;
    private final InterviewVideoStorage interviewVideoStorage;
    private final UtteranceSegmentRepository utteranceSegmentRepository;
    private final GuestFeedbackReportQueryUseCase guestFeedbackReportQueryUseCase;

    @Override
    @Transactional(readOnly = true)
    public InterviewReportQueryResult getReport(UUID userId, Long sessionId) {
        interviewSessionOwnershipCheckUseCase.requireOwned(userId, sessionId);

        // 채점 파이프라인은 완료 시점에만 리포트 row를 저장한다. row가 아직 없으면 생성 중이다.
        Report report = reportRepository.findBySessionId(sessionId).orElse(null);
        if (report == null) {
            return statusOnly(ReportStatus.GENERATING);
        }
        if (report.getStatus() == ReportStatus.FAILED) {
            return statusOnly(ReportStatus.FAILED);
        }

        // 채점(report row)만으로는 부족하다 — 영상 합성(composite)까지 끝나야 READY로 노출한다(#155).
        // 합성이 timeout을 넘겨도 안 끝나면(업로드 누락 등) 더는 기다리지 않고 영상 없이 진행한다.
        InterviewVideo video = interviewVideoRepository.findBySessionId(sessionId).orElse(null);
        if (report.effectiveStatus(video) == ReportStatus.GENERATING) {
            return statusOnly(ReportStatus.GENERATING);
        }

        List<RedFlag> redFlags = redFlagRepository.findAllBySessionId(sessionId);
        List<ReportCard> cards = reportCardRepository.findAllBySessionId(sessionId);

        Map<Long, String> questionContentById = new HashMap<>();
        questionRepository.findAllBySessionId(sessionId)
                .forEach(question -> questionContentById.put(question.getId(), question.getContent()));
        // 스킵된 답변은 sttText가 null일 수 있어 Collectors.toMap(null 값 NPE) 대신 수동으로 담는다.
        Map<Long, String> transcriptByQuestionId = new HashMap<>();
        answerRepository.findAllBySessionId(sessionId)
                .forEach(answer -> transcriptByQuestionId.putIfAbsent(answer.getQuestionId(), answer.getSttText()));

        Map<TestType, Integer> axisOrderByType = computeAxisOrder(cards);
        Map<TestType, ResolutionLowReason> lowReasonByAxis = lowResolutionByAxis(sessionId);
        Map<TestType, List<InterviewReportQueryResult.RedFlagNotice>> cardNoticesByAxis = cardNoticesByAxis(redFlags);
        // 특정 축이 없는(CONTRADICTION 등) 노출 레드플래그는 근거 questionId로 해당 카드에 직접 붙인다.
        Map<Long, List<InterviewReportQueryResult.RedFlagNotice>> cardNoticesByQuestionId = cardNoticesByQuestionId(redFlags);
        // 문장 단위 발화 시각(면접관/면접자)을 questionId별로 묶어 카드에 붙인다(#78). 각 리스트는 startSec 순(면접관 → 면접자).
        Map<Long, List<UtteranceSegment>> segmentsByQuestionId = utteranceSegmentRepository.findBySessionIdGroupedByQuestionId(sessionId);
        // 카드(채점 대상 턴) 유무와 무관하게, 세션의 모든 발화를 startSec 순으로 이어붙인 전체 대본 타임라인(영상 싱크용).
        List<InterviewReportQueryResult.ScriptLine> script = new ArrayList<>(segmentsByQuestionId.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(UtteranceSegment::startSec))
                .map(segment -> new InterviewReportQueryResult.ScriptLine(
                        segment.role(), segment.text(), segment.startSec(), segment.endSec()))
                .toList());
        // 면접관 마무리 멘트는 질문(Question)이 아니라 세그먼트로 저장되지 않으므로, 종료 유형별 고정 문구를
        // 프론트가 보고한 재생 구간(video.wrapUp*)에 얹어 대본 맨 끝에 이어붙인다. 마무리 멘트가 없으면 생략.
        appendWrapUpLine(script, video, sessionId);

        List<InterviewReportQueryResult.Card> cardResults = cards.stream()
                .sorted(Comparator
                        .comparingInt((ReportCard c) -> axisOrderByType.getOrDefault(c.getTestType(), Integer.MAX_VALUE))
                        .thenComparingInt(ReportCard::getDepthLevel)
                        .thenComparing(ReportCard::getQuestionId))
                .map(card -> toCard(card, axisOrderByType, lowReasonByAxis, cardNoticesByAxis, cardNoticesByQuestionId, questionContentById, transcriptByQuestionId, segmentsByQuestionId))
                .toList();

        // 질문 음성 합성이 끝났고(composited) 아직 만료 전일 때만 재생 URL(final.mp4)을 발급한다.
        // 합성 전/실패 시에는 null (원본 raw.mp4 폴백은 하지 않는다).
        InterviewReportQueryResult.Video videoResult = video == null ? null : new InterviewReportQueryResult.Video(
                video.isComposited() && !video.isExpired() ? interviewVideoStorage.presignComposite(userId, sessionId) : null,
                video.isExpired(),
                video.getExpiresAt());

        return new InterviewReportQueryResult(
                report.getStatus(),
                report.getHeadline(),
                videoResult,
                cardResults,
                script,
                toGuestSection(guestFeedbackReportQueryUseCase.getForReport(sessionId))
        );
    }

    // 마무리 멘트를 전체 대본 끝에 이어붙인다. 프론트가 재생 구간(wrapUpStartSec)을 보고했고 종료 유형에 마무리 문구가 있을 때만.
    // endSec은 보고값이 없으면 startSec으로 대체(재생 위치 강조가 순간으로만 잡힐 뿐 대본 노출엔 영향 없음).
    private void appendWrapUpLine(List<InterviewReportQueryResult.ScriptLine> script, InterviewVideo video, Long sessionId) {
        if (video == null || video.getWrapUpStartSec() == null) {
            return;
        }
        InterviewEndType endType = interviewSessionRepository.findById(sessionId)
                .map(session -> session.getEndType())
                .orElse(null);
        String text = WrapUpMessage.textFor(endType);
        if (text == null) {
            return;
        }
        float startSec = video.getWrapUpStartSec();
        float endSec = video.getWrapUpEndSec() != null ? video.getWrapUpEndSec() : startSec;
        script.add(new InterviewReportQueryResult.ScriptLine(ScriptRole.INTERVIEWER, text, startSec, endSec));
    }

    private InterviewReportQueryResult statusOnly(ReportStatus status) {
        return new InterviewReportQueryResult(status, null, null, null, null, null);
    }

    // 카드를 축(testType)별 최소 questionId 순으로 줄세워, 면접에서 그 축이 다뤄진 순서(1부터)를 매긴다.
    private Map<TestType, Integer> computeAxisOrder(List<ReportCard> cards) {
        Map<TestType, Long> firstQuestionId = new EnumMap<>(TestType.class);
        for (ReportCard card : cards) {
            firstQuestionId.merge(card.getTestType(), card.getQuestionId(), Math::min);
        }
        List<TestType> ordered = firstQuestionId.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
        Map<TestType, Integer> axisOrder = new EnumMap<>(TestType.class);
        for (int i = 0; i < ordered.size(); i++) {
            axisOrder.put(ordered.get(i), i + 1);
        }
        return axisOrder;
    }

    private Map<TestType, ResolutionLowReason> lowResolutionByAxis(Long sessionId) {
        Map<TestType, ResolutionLowReason> lowByAxis = new EnumMap<>(TestType.class);
        for (AxisEvaluation evaluation : axisEvaluationRepository.findAllBySessionId(sessionId)) {
            if (evaluation.getResolutionLevel() == ResolutionLevel.LOW) {
                lowByAxis.put(evaluation.getTestType(), evaluation.getResolutionLowReason());
            }
        }
        return lowByAxis;
    }

    // 축 단위 노출 레드플래그를 그 축의 카드 전부에 붙인다. affectedTestType이 없는(CONTRADICTION) 건은 카드에 붙이지 않는다.
    private Map<TestType, List<InterviewReportQueryResult.RedFlagNotice>> cardNoticesByAxis(List<RedFlag> redFlags) {
        Map<TestType, List<InterviewReportQueryResult.RedFlagNotice>> byAxis = new EnumMap<>(TestType.class);
        redFlags.stream()
                .filter(redFlag -> redFlag.getType().isExposed())
                .filter(redFlag -> redFlag.getAffectedTestType() != null)
                .forEach(redFlag -> byAxis
                        .computeIfAbsent(redFlag.getAffectedTestType(), k -> new ArrayList<>())
                        .add(new InterviewReportQueryResult.RedFlagNotice(redFlag.getType(), RED_FLAG_NOTICE.get(redFlag.getType()))));
        // 같은 축에 같은 유형이 중복 확정될 수 있으니 유형 기준으로 정리한다.
        byAxis.replaceAll((axis, notices) -> notices.stream().distinct().toList());
        return byAxis;
    }

    // 특정 축에 매이지 않는(affectedTestType 없는) 노출 레드플래그(예: CONTRADICTION)를 근거 questionId별로
    // 묶어, 그 카드에 직접 붙인다. 축 기반 레드플래그는 cardNoticesByAxis가 담당하므로 여기서 제외한다.
    private Map<Long, List<InterviewReportQueryResult.RedFlagNotice>> cardNoticesByQuestionId(List<RedFlag> redFlags) {
        Map<Long, List<InterviewReportQueryResult.RedFlagNotice>> byQuestion = new HashMap<>();
        redFlags.stream()
                .filter(redFlag -> redFlag.getType().isExposed())
                .filter(redFlag -> redFlag.getAffectedTestType() == null)
                .forEach(redFlag -> redFlag.getRelatedQuestionIds().forEach(questionId -> byQuestion
                        .computeIfAbsent(questionId, k -> new ArrayList<>())
                        .add(new InterviewReportQueryResult.RedFlagNotice(redFlag.getType(), RED_FLAG_NOTICE.get(redFlag.getType())))));
        byQuestion.replaceAll((questionId, notices) -> notices.stream().distinct().toList());
        return byQuestion;
    }

    // 축 기반 + 질문 기반 노출 레드플래그 안내를 유형 기준으로 합쳐 하나도 없으면 null을 반환한다(빈 배열 아님).
    private List<InterviewReportQueryResult.RedFlagNotice> mergeCardNotices(
            List<InterviewReportQueryResult.RedFlagNotice> axisNotices,
            List<InterviewReportQueryResult.RedFlagNotice> questionNotices
    ) {
        Map<RedFlagType, InterviewReportQueryResult.RedFlagNotice> byType = new LinkedHashMap<>();
        if (axisNotices != null) {
            axisNotices.forEach(notice -> byType.putIfAbsent(notice.type(), notice));
        }
        if (questionNotices != null) {
            questionNotices.forEach(notice -> byType.putIfAbsent(notice.type(), notice));
        }
        return byType.isEmpty() ? null : List.copyOf(byType.values());
    }

    private InterviewReportQueryResult.Card toCard(
            ReportCard card,
            Map<TestType, Integer> axisOrderByType,
            Map<TestType, ResolutionLowReason> lowReasonByAxis,
            Map<TestType, List<InterviewReportQueryResult.RedFlagNotice>> cardNoticesByAxis,
            Map<Long, List<InterviewReportQueryResult.RedFlagNotice>> cardNoticesByQuestionId,
            Map<Long, String> questionContentById,
            Map<Long, String> transcriptByQuestionId,
            Map<Long, List<UtteranceSegment>> segmentsByQuestionId
    ) {
        ResolutionLowReason lowReason = lowReasonByAxis.get(card.getTestType());
        String resolutionNotice = lowReason == null ? null : RESOLUTION_NOTICE.get(lowReason);

        List<UtteranceSegment> cardSegments = segmentsByQuestionId.getOrDefault(card.getQuestionId(), List.of());
        // 하이라이트 startIndex는 answer 대본(transcript) 기준이므로, 같은 좌표계인 INTERVIEWEE 세그먼트만으로
        // "영상 보러가기" 시각을 찾는다. startIndex 오름차순으로 정렬해 floor 매칭(findHighlightStartSec)에 쓴다.
        List<UtteranceSegment> answerSegments = cardSegments.stream()
                .filter(segment -> segment.role() == ScriptRole.INTERVIEWEE)
                .sorted(Comparator.comparingInt(UtteranceSegment::startIndex))
                .toList();

        List<InterviewReportQueryResult.HighlightSpan> highlightSpans = card.getHighlightSpans().stream()
                .map(span -> {
                    // OFF_INTENT(딴 답)일 때만 "질문 의도 ↔ 내 답변" 대비용 3필드를 채운다. 그 외 reason은 null.
                    boolean offIntent = span.reason() == HighlightReason.OFF_INTENT;
                    return new InterviewReportQueryResult.HighlightSpan(
                            span.range().startIndex(), span.range().endIndex(), span.tone(), span.reason(),
                            span.title(), span.analysis(), span.followUpQuestions(),
                            findHighlightStartSec(span.range().startIndex(), answerSegments),
                            offIntent ? span.answerTopicTitle() : null,
                            offIntent ? card.getQuestionIntentTitle() : null,
                            offIntent ? card.getQuestionIntentTranslation() : null);
                })
                .toList();

        List<InterviewReportQueryResult.ScriptSegment> scriptSegments = cardSegments.stream()
                .map(segment -> new InterviewReportQueryResult.ScriptSegment(
                        segment.role(), segment.text(), segment.startIndex(), segment.endIndex(),
                        segment.startSec(), segment.endSec()))
                .toList();

        return new InterviewReportQueryResult.Card(
                axisOrderByType.getOrDefault(card.getTestType(), 0),
                card.getDepthLevel(),
                questionContentById.get(card.getQuestionId()),
                transcriptByQuestionId.get(card.getQuestionId()),
                highlightSpans,
                resolutionNotice,
                // 축 기반(cardNoticesByAxis) + 이 질문에 직접 걸린 축 없는 레드플래그(cardNoticesByQuestionId)를 합친다.
                // 하나도 없으면 빈 배열이 아니라 null로 내린다(계약: null 유무로 비어있음 판단).
                mergeCardNotices(cardNoticesByAxis.get(card.getTestType()), cardNoticesByQuestionId.get(card.getQuestionId())),
                card.getQuestionIntentTitle(),
                card.getQuestionIntentTranslation(),
                scriptSegments
        );
    }

    // 하이라이트 시작 인덱스 이하로 시작하는 세그먼트 중 가장 뒤(가장 큰 startIndex)의 startSec을 쓴다(floor 매칭).
    // 세그먼트 경계에 정확히 포함되지 않아도(근사 배치 등으로 생기는 작은 틈) 항상 가장 가까운 답을 찾는다.
    // answerSegmentsAscending은 startIndex 오름차순이어야 한다. 세그먼트가 하나도 없으면 null.
    private Float findHighlightStartSec(int highlightStartIndex, List<UtteranceSegment> answerSegmentsAscending) {
        Float startSec = null;
        for (UtteranceSegment segment : answerSegmentsAscending) {
            if (segment.startIndex() > highlightStartIndex) {
                break;
            }
            startSec = segment.startSec();
        }
        return startSec;
    }

    private InterviewReportQueryResult.GuestFeedbackSection toGuestSection(GuestFeedbackReportView view) {
        // 지인이 한 명도 없어도 섹션은 항상 내려준다(participantCount=0, guests=[]).
        // 프론트가 null 체크 없이 바로 guests를 순회하고, "아직 참여 없음"을 participantCount==0으로 표현하게 한다.
        if (view.participantCount() == 0) {
            return new InterviewReportQueryResult.GuestFeedbackSection(0, List.of());
        }
        List<InterviewReportQueryResult.Guest> guests = view.guests().stream()
                .map(guest -> new InterviewReportQueryResult.Guest(
                        guest.alias(),
                        guest.ratings().stream()
                                .map(rating -> new InterviewReportQueryResult.AttitudeRating(rating.axis(), rating.level(), rating.comment()))
                                .toList()))
                .toList();
        return new InterviewReportQueryResult.GuestFeedbackSection(view.participantCount(), guests);
    }
}
