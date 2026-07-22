package com.yapp.d14.interview.application.service;

import com.yapp.d14.feedback.application.port.in.GuestFeedbackReportQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackReportView;
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
import com.yapp.d14.interview.domain.HeadlineBranch;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.ResolutionLowReason;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TextRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InterviewReportQueryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long SESSION_ID = 100L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 21, 10, 0);

    @Mock
    private InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportCardRepository reportCardRepository;
    @Mock
    private RedFlagRepository redFlagRepository;
    @Mock
    private AxisEvaluationRepository axisEvaluationRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private InterviewVideoRepository interviewVideoRepository;
    @Mock
    private InterviewVideoStorage interviewVideoStorage;
    @Mock
    private GuestFeedbackReportQueryUseCase guestFeedbackReportQueryUseCase;

    @InjectMocks
    private InterviewReportQueryService service;

    @Test
    void 리포트_row가_없으면_GENERATING만_반환한다() {
        given(reportRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.status()).isEqualTo(ReportStatus.GENERATING);
        assertThat(result.headline()).isNull();
        assertThat(result.cards()).isNull();
        assertThat(result.video()).isNull();
        assertThat(result.guestFeedback()).isNull();
        verify(interviewSessionOwnershipCheckUseCase).requireOwned(USER_ID, SESSION_ID);
        verifyNoInteractions(reportCardRepository, redFlagRepository, guestFeedbackReportQueryUseCase);
    }

    @Test
    void FAILED_상태면_status만_반환한다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.FAILED, null)));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.status()).isEqualTo(ReportStatus.FAILED);
        assertThat(result.cards()).isNull();
        verifyNoInteractions(reportCardRepository, guestFeedbackReportQueryUseCase);
    }

    @Test
    void READY면_카드를_축순서_depth순으로_조립하고_영상url은_null이다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "한 줄 요약")));

        // BOUNDARY가 먼저 등장(questionId 10), DEPTH는 뒤(questionId 20,21) → axisOrder: BOUNDARY=1, DEPTH=2
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                card(1L, 21L, 2, TestType.DEPTH, "깊이 의도2", List.of()),
                card(2L, 10L, 1, TestType.BOUNDARY, "경계 의도", List.of(
                        new HighlightSpan(new TextRange(0, 3), HighlightTone.GOOD, "좋은 근거", List.of("추가 질문1", "추가 질문2")))),
                card(3L, 20L, 1, TestType.DEPTH, "깊이 의도1", List.of())
        ));
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question(10L, "경계 질문"), question(20L, "깊이 질문1"), question(21L, "깊이 질문2")
        ));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                answer(10L, "경계 답변"), answer(20L, "깊이 답변1"), answer(21L, null)
        ));
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                axisEval(TestType.DEPTH, ResolutionLevel.LOW, ResolutionLowReason.SHALLOW_ANSWER),
                axisEval(TestType.BOUNDARY, ResolutionLevel.NORMAL, null)
        ));
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        // 업로드 전(uploaded=false)이므로 재생 URL은 발급되지 않는다.
        given(interviewVideoRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(InterviewVideo.of(1L, SESSION_ID, NOW, NOW.plusDays(3), false, false)));
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.status()).isEqualTo(ReportStatus.READY);
        assertThat(result.headline()).isEqualTo("한 줄 요약");
        assertThat(result.guestFeedback()).isNull();

        assertThat(result.video().url()).isNull();
        assertThat(result.video().expired()).isFalse();
        assertThat(result.video().expiresAt()).isEqualTo(NOW.plusDays(3));

        List<InterviewReportQueryResult.Card> cards = result.cards();
        assertThat(cards).extracting(InterviewReportQueryResult.Card::axisOrder, InterviewReportQueryResult.Card::depthLevel)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, 1), // BOUNDARY 1-1
                        org.assertj.core.groups.Tuple.tuple(2, 1), // DEPTH 2-1
                        org.assertj.core.groups.Tuple.tuple(2, 2)  // DEPTH 2-2
                );

        InterviewReportQueryResult.Card boundaryCard = cards.get(0);
        assertThat(boundaryCard.questionText()).isEqualTo("경계 질문");
        assertThat(boundaryCard.transcript()).isEqualTo("경계 답변");
        assertThat(boundaryCard.resolutionNotice()).isNull();
        assertThat(boundaryCard.highlightSpans()).hasSize(1);
        assertThat(boundaryCard.highlightSpans().get(0).followUpQuestions())
                .containsExactly("추가 질문1", "추가 질문2");

        InterviewReportQueryResult.Card depthCard = cards.get(1);
        assertThat(depthCard.resolutionNotice()).isNotNull();
        assertThat(depthCard.highlightSpans()).isEmpty();
        assertThat(cards.get(2).transcript()).isNull(); // 스킵된 답변(null STT)도 NPE 없이 처리
    }

    @Test
    void 노출_레드플래그는_top_level과_해당_축_카드에_붙고_CONTRADICTION은_카드에_안붙는다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                card(1L, 10L, 1, TestType.DEPTH, "의도", List.of())
        ));
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(question(10L, "질문")));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(answer(10L, "답변")));
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                redFlag(RedFlagType.FABRICATION, TestType.DEPTH),   // 노출 + DEPTH
                redFlag(RedFlagType.CONTRADICTION, null),           // 노출 + 축없음 → top-level만
                redFlag(RedFlagType.BUZZWORD_SALAD, TestType.DEPTH) // 비노출 → 제외
        ));
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.redFlagNotices())
                .extracting(InterviewReportQueryResult.RedFlagNotice::type)
                .containsExactly(RedFlagType.FABRICATION, RedFlagType.CONTRADICTION);
        assertThat(result.video()).isNull();

        List<InterviewReportQueryResult.RedFlagNotice> cardNotices = result.cards().get(0).cardRedFlagNotices();
        assertThat(cardNotices).extracting(InterviewReportQueryResult.RedFlagNotice::type)
                .containsExactly(RedFlagType.FABRICATION);
    }

    @Test
    void 지인_피드백이_있으면_섹션을_조립한다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID)).willReturn(
                new GuestFeedbackReportView(1, List.of(
                        new GuestFeedbackReportView.Guest("친구A", List.of(
                                new GuestFeedbackReportView.Rating("GAZE", 3, "안정적")))))
        );

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.guestFeedback().participantCount()).isEqualTo(1);
        assertThat(result.guestFeedback().guests()).hasSize(1);
        InterviewReportQueryResult.Guest guest = result.guestFeedback().guests().get(0);
        assertThat(guest.alias()).isEqualTo("친구A");
        assertThat(guest.attitudeRatings()).singleElement()
                .satisfies(rating -> {
                    assertThat(rating.axis()).isEqualTo("GAZE");
                    assertThat(rating.level()).isEqualTo(3);
                    assertThat(rating.comment()).isEqualTo("안정적");
                });
    }

    private Report report(ReportStatus status, String headline) {
        return Report.of(1L, SESSION_ID, 80.0, null, headline, HeadlineBranch.NORMAL, status, NOW);
    }

    private ReportCard card(Long id, Long questionId, int depthLevel, TestType testType, String intent, List<HighlightSpan> spans) {
        return ReportCard.of(id, SESSION_ID, questionId, depthLevel, testType, intent, spans, NOW);
    }

    private Question question(Long id, String content) {
        return Question.of(id, SESSION_ID, content, 1, 1, TestType.DEPTH, null, null, null, null, false, NOW);
    }

    private Answer answer(Long questionId, String sttText) {
        return Answer.of(questionId, SESSION_ID, questionId, sttText, null, null, null,
                sttText == null, null, null, null, null, false, false, TestType.DEPTH, NOW);
    }

    private AxisEvaluation axisEval(TestType testType, ResolutionLevel level, ResolutionLowReason reason) {
        return AxisEvaluation.of(1L, SESSION_ID, testType, 3, null, level, reason, List.of(), "근거", NOW);
    }

    private RedFlag redFlag(RedFlagType type, TestType affectedTestType) {
        return RedFlag.of(1L, SESSION_ID, type, affectedTestType, null, false, List.of(), NOW);
    }
}
