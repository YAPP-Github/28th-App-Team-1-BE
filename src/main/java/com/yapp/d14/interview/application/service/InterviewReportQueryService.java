package com.yapp.d14.interview.application.service;

import com.yapp.d14.feedback.application.port.in.GuestFeedbackReportQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackReportView;
import com.yapp.d14.interview.application.port.in.InterviewReportQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportQueryResult;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.AxisEvaluationRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.RedFlagRepository;
import com.yapp.d14.interview.application.port.out.ReportCardRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.ResolutionLowReason;
import com.yapp.d14.interview.domain.TestType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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

    // 헤드라인 아래 안내 줄은 최대 2줄까지만 노출한다.
    private static final int MAX_TOP_LEVEL_NOTICES = 2;

    private final InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    private final ReportRepository reportRepository;
    private final ReportCardRepository reportCardRepository;
    private final RedFlagRepository redFlagRepository;
    private final AxisEvaluationRepository axisEvaluationRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final InterviewVideoRepository interviewVideoRepository;
    private final InterviewVideoStorage interviewVideoStorage;
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

        List<InterviewReportQueryResult.Card> cardResults = cards.stream()
                .sorted(Comparator
                        .comparingInt((ReportCard c) -> axisOrderByType.getOrDefault(c.getTestType(), Integer.MAX_VALUE))
                        .thenComparingInt(ReportCard::getDepthLevel)
                        .thenComparing(ReportCard::getQuestionId))
                .map(card -> toCard(card, axisOrderByType, lowReasonByAxis, cardNoticesByAxis, questionContentById, transcriptByQuestionId))
                .toList();

        InterviewReportQueryResult.Video video = interviewVideoRepository.findBySessionId(sessionId)
                .map(v -> {
                    // 업로드가 끝났고(uploaded) 아직 만료 전일 때만 재생 URL을 발급한다.
                    String url = v.isUploaded() && !v.isExpired()
                            ? interviewVideoStorage.presignPlayback(userId, sessionId)
                            : null;
                    return new InterviewReportQueryResult.Video(url, v.isExpired(), v.getExpiresAt());
                })
                .orElse(null);

        return new InterviewReportQueryResult(
                report.getStatus(),
                report.getHeadline(),
                topLevelNotices(redFlags),
                video,
                cardResults,
                toGuestSection(guestFeedbackReportQueryUseCase.getForReport(sessionId))
        );
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

    // 노출 레드플래그가 없으면 빈 배열이 아니라 null로 내린다(계약: "비어 있는지"를 null 유무로 판단, GENERATING/FAILED와 동일).
    private List<InterviewReportQueryResult.RedFlagNotice> topLevelNotices(List<RedFlag> redFlags) {
        List<InterviewReportQueryResult.RedFlagNotice> notices = redFlags.stream()
                .map(RedFlag::getType)
                .filter(RedFlagType::isExposed)
                .distinct()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .limit(MAX_TOP_LEVEL_NOTICES)
                .map(type -> new InterviewReportQueryResult.RedFlagNotice(type, RED_FLAG_NOTICE.get(type)))
                .toList();
        return notices.isEmpty() ? null : notices;
    }

    private InterviewReportQueryResult.Card toCard(
            ReportCard card,
            Map<TestType, Integer> axisOrderByType,
            Map<TestType, ResolutionLowReason> lowReasonByAxis,
            Map<TestType, List<InterviewReportQueryResult.RedFlagNotice>> cardNoticesByAxis,
            Map<Long, String> questionContentById,
            Map<Long, String> transcriptByQuestionId
    ) {
        ResolutionLowReason lowReason = lowReasonByAxis.get(card.getTestType());
        String resolutionNotice = lowReason == null ? null : RESOLUTION_NOTICE.get(lowReason);

        List<InterviewReportQueryResult.HighlightSpan> highlightSpans = card.getHighlightSpans().stream()
                .map(span -> new InterviewReportQueryResult.HighlightSpan(
                        span.range().startIndex(), span.range().endIndex(), span.tone(), span.analysis(), span.followUpQuestions()))
                .toList();

        return new InterviewReportQueryResult.Card(
                axisOrderByType.getOrDefault(card.getTestType(), 0),
                card.getDepthLevel(),
                questionContentById.get(card.getQuestionId()),
                transcriptByQuestionId.get(card.getQuestionId()),
                highlightSpans,
                resolutionNotice,
                // 이 카드(축)에 걸린 노출 레드플래그가 없으면 빈 배열이 아니라 null로 내린다(top-level과 동일 규약).
                cardNoticesByAxis.get(card.getTestType()),
                card.getQuestionIntentTranslation()
        );
    }

    private InterviewReportQueryResult.GuestFeedbackSection toGuestSection(GuestFeedbackReportView view) {
        if (view.participantCount() == 0) {
            return null;
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
