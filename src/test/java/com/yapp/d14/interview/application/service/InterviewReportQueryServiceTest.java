package com.yapp.d14.interview.application.service;

import com.yapp.d14.feedback.application.port.in.GuestFeedbackReportQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackReportView;
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
import com.yapp.d14.interview.domain.HeadlineBranch;
import com.yapp.d14.interview.domain.HighlightReason;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.InterviewEndType;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.ResolutionLowReason;
import com.yapp.d14.interview.domain.ScriptRole;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TextRange;
import com.yapp.d14.interview.domain.UtteranceSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
    private InterviewSessionRepository interviewSessionRepository;
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
    private UtteranceSegmentRepository utteranceSegmentRepository;
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
                        new HighlightSpan(new TextRange(0, 3), HighlightTone.GOOD, HighlightReason.PROBE_WORTHY, "좋은 근거 제시", "좋은 근거", List.of("추가 질문1", "추가 질문2"), null))),
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
        // expired()는 실제 LocalDateTime.now() 기준으로 계산되므로, 고정 날짜 대신 현재 기준 미래값을 사용한다.
        LocalDateTime videoExpiresAt = LocalDateTime.now().plusDays(3);
        given(interviewVideoRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(InterviewVideo.of(1L, SESSION_ID, NOW, videoExpiresAt, false, false, false, null, null)));
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.status()).isEqualTo(ReportStatus.READY);
        assertThat(result.headline()).isEqualTo("한 줄 요약");
        // 지인이 0명이어도 섹션은 null이 아니라 빈 섹션(participantCount=0, guests=[])으로 내려온다.
        assertThat(result.guestFeedback()).isNotNull();
        assertThat(result.guestFeedback().participantCount()).isZero();
        assertThat(result.guestFeedback().guests()).isEmpty();

        assertThat(result.video().url()).isNull();
        assertThat(result.video().expired()).isFalse();
        assertThat(result.video().expiresAt()).isEqualTo(videoExpiresAt);

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
    void 문장_발화_시각을_questionId로_해당_카드에_붙인다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                card(1L, 10L, 1, TestType.DEPTH, "의도", List.of())
        ));
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(question(10L, "질문입니다.")));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(answer(10L, "답변입니다.")));
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                axisEval(TestType.DEPTH, ResolutionLevel.NORMAL, null)
        ));
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));
        // 저장된 세그먼트는 questionId 10의 카드에 startSec 순(질문 → 답변)으로 붙어야 한다.
        given(utteranceSegmentRepository.findBySessionIdGroupedByQuestionId(SESSION_ID)).willReturn(Map.of(
                10L, List.of(
                        new UtteranceSegment(ScriptRole.INTERVIEWER, "질문입니다.", 0, 6, 12.0f, 14.0f),
                        new UtteranceSegment(ScriptRole.INTERVIEWEE, "답변입니다.", 0, 6, 15.0f, 17.0f)
                )
        ));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        List<InterviewReportQueryResult.ScriptSegment> segments = result.cards().get(0).scriptSegments();
        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).role()).isEqualTo(ScriptRole.INTERVIEWER);
        assertThat(segments.get(0).text()).isEqualTo("질문입니다.");
        assertThat(segments.get(0).startSec()).isEqualTo(12.0f);
        assertThat(segments.get(0).endSec()).isEqualTo(14.0f);
        assertThat(segments.get(1).role()).isEqualTo(ScriptRole.INTERVIEWEE);
        assertThat(segments.get(1).startSec()).isEqualTo(15.0f);
    }

    @Test
    void 하이라이트_시작지점의_영상_재생시각을_답변세그먼트에서_floor매칭으로_찾는다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                card(1L, 10L, 1, TestType.DEPTH, "의도", List.of(
                        // startIndex=5 → 답변 세그먼트 [0,8) 안에 포함
                        new HighlightSpan(new TextRange(5, 8), HighlightTone.GOOD, HighlightReason.SUFFICIENT, "제목1", "분석1", List.of(), null),
                        // startIndex=12 → 답변 세그먼트 [8,20) 안에 포함
                        new HighlightSpan(new TextRange(12, 15), HighlightTone.GOOD, HighlightReason.SUFFICIENT, "제목2", "분석2", List.of(), null)
                ))
        ));
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(question(10L, "질문")));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(answer(10L, "답변 전체 문자열입니다")));
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                axisEval(TestType.DEPTH, ResolutionLevel.NORMAL, null)
        ));
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));
        given(utteranceSegmentRepository.findBySessionIdGroupedByQuestionId(SESSION_ID)).willReturn(Map.of(
                10L, List.of(
                        new UtteranceSegment(ScriptRole.INTERVIEWER, "질문", 0, 2, 1.0f, 2.0f),
                        new UtteranceSegment(ScriptRole.INTERVIEWEE, "0~8구간", 0, 8, 20.0f, 25.0f),
                        new UtteranceSegment(ScriptRole.INTERVIEWEE, "8~20구간", 8, 20, 26.0f, 33.0f)
                )
        ));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        List<InterviewReportQueryResult.HighlightSpan> highlights = result.cards().get(0).highlightSpans();
        assertThat(highlights.get(0).startSec()).isEqualTo(20.0f);
        assertThat(highlights.get(1).startSec()).isEqualTo(26.0f);
    }

    @Test
    void 답변_세그먼트가_없으면_하이라이트_startSec은_null이다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                card(1L, 10L, 1, TestType.DEPTH, "의도", List.of(
                        new HighlightSpan(new TextRange(0, 3), HighlightTone.GOOD, HighlightReason.SUFFICIENT, "제목", "분석", List.of(), null)
                ))
        ));
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(question(10L, "질문")));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(answer(10L, "답변")));
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                axisEval(TestType.DEPTH, ResolutionLevel.NORMAL, null)
        ));
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));
        given(utteranceSegmentRepository.findBySessionIdGroupedByQuestionId(SESSION_ID)).willReturn(Map.of());

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.cards().get(0).highlightSpans().get(0).startSec()).isNull();
    }

    @Test
    void 전체_대본_script는_카드없는_질문의_발화까지_startSec순으로_모두_담는다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        // 카드는 질문 20(중간 턴) 하나뿐이지만, 첫 질문(10)·마지막 답변(30)은 카드가 없다.
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                card(1L, 20L, 1, TestType.DEPTH, "의도", List.of())
        ));
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(question(20L, "중간 질문")));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(answer(20L, "중간 답변")));
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                axisEval(TestType.DEPTH, ResolutionLevel.NORMAL, null)
        ));
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.empty());
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));
        given(utteranceSegmentRepository.findBySessionIdGroupedByQuestionId(SESSION_ID)).willReturn(Map.of(
                10L, List.of(new UtteranceSegment(ScriptRole.INTERVIEWER, "첫 면접관 멘트", 0, 7, 5.0f, 8.0f)),   // 카드 없음
                20L, List.of(
                        new UtteranceSegment(ScriptRole.INTERVIEWER, "중간 질문", 0, 5, 30.0f, 32.0f),
                        new UtteranceSegment(ScriptRole.INTERVIEWEE, "중간 답변", 0, 5, 33.0f, 36.0f)),
                30L, List.of(new UtteranceSegment(ScriptRole.INTERVIEWEE, "마지막 멘트", 0, 6, 60.0f, 63.0f))     // 카드 없음
        ));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        // 카드는 하나뿐이라도 전체 대본은 카드 없는 첫 멘트·마지막 멘트까지 startSec 순으로 담는다.
        assertThat(result.script())
                .extracting(InterviewReportQueryResult.ScriptLine::text)
                .containsExactly("첫 면접관 멘트", "중간 질문", "중간 답변", "마지막 멘트");
        assertThat(result.script())
                .extracting(InterviewReportQueryResult.ScriptLine::startSec)
                .containsExactly(5.0f, 30.0f, 33.0f, 60.0f);
    }

    @Test
    void 마무리_멘트_재생구간이_있으면_종료문구를_대본_끝에_INTERVIEWER로_이어붙인다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                card(1L, 20L, 1, TestType.DEPTH, "의도", List.of())
        ));
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(question(20L, "중간 질문")));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(answer(20L, "중간 답변")));
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                axisEval(TestType.DEPTH, ResolutionLevel.NORMAL, null)
        ));
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        // 마무리 멘트 재생 구간(70~73초)이 저장돼 있고 종료 유형이 NORMAL_END면 해당 문구가 대본 끝에 붙는다.
        given(interviewVideoRepository.findBySessionId(SESSION_ID)).willReturn(Optional.of(
                InterviewVideo.of(1L, SESSION_ID, NOW, LocalDateTime.now().plusDays(3), false, true, false, 70.0f, 73.0f)));
        InterviewSession session = mock(InterviewSession.class);
        given(session.getEndType()).willReturn(InterviewEndType.NORMAL_END);
        given(interviewSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));
        given(utteranceSegmentRepository.findBySessionIdGroupedByQuestionId(SESSION_ID)).willReturn(Map.of(
                20L, List.of(
                        new UtteranceSegment(ScriptRole.INTERVIEWER, "중간 질문", 0, 5, 30.0f, 32.0f),
                        new UtteranceSegment(ScriptRole.INTERVIEWEE, "중간 답변", 0, 5, 33.0f, 36.0f))
        ));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        List<InterviewReportQueryResult.ScriptLine> script = result.script();
        InterviewReportQueryResult.ScriptLine last = script.get(script.size() - 1);
        assertThat(last.role()).isEqualTo(ScriptRole.INTERVIEWER);
        assertThat(last.text()).isEqualTo("수고하셨습니다. 오늘 면접은 여기까지입니다.");
        assertThat(last.startSec()).isEqualTo(70.0f);
        assertThat(last.endSec()).isEqualTo(73.0f);
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
    void 영상이_업로드됐고_만료전이면_재생url을_발급한다() {
        given(reportRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(report(ReportStatus.READY, "요약")));
        given(reportCardRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(axisEvaluationRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        given(redFlagRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        // expired()는 실제 LocalDateTime.now() 기준으로 계산되므로, 고정 날짜 대신 현재 기준 미래값을 사용한다.
        given(interviewVideoRepository.findBySessionId(SESSION_ID))
                .willReturn(Optional.of(InterviewVideo.of(1L, SESSION_ID, NOW, LocalDateTime.now().plusDays(3), false, true, true, null, null)));
        given(interviewVideoStorage.presignComposite(USER_ID, SESSION_ID)).willReturn("https://s3/play");
        given(guestFeedbackReportQueryUseCase.getForReport(SESSION_ID))
                .willReturn(new GuestFeedbackReportView(0, List.of()));

        InterviewReportQueryResult result = service.getReport(USER_ID, SESSION_ID);

        assertThat(result.video().url()).isEqualTo("https://s3/play");
        assertThat(result.video().expired()).isFalse();
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
        return ReportCard.of(id, SESSION_ID, questionId, depthLevel, testType, "제목", intent, spans, NOW);
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
