package com.yapp.d14.interview.tool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 리포트 조회 API(GET /api/v1/interview/sessions/{id}/report)의 "가능한 모든 응답 케이스"를 한 유저에게 심는다.
 * 프론트가 실제 API로 카드/하이라이트/레드플래그/해상도낮음/스크립트/지인 피드백/상태값의 모든 분기를 확인할 수 있게 하기 위함이다.
 * 실제 면접·채점 파이프라인을 거치지 않고 DB에 직접 픽스처를 넣는다. 영상 원본은 만들지 않으므로 재생 URL은 항상 null이다.
 *
 * ./gradlew seedReportShowcase [-PuserId=<uuid>]   (기본 userId = 00000000-0000-0000-0000-000000000001)
 */
public class ReportShowcaseSeedCli {

    private static final UUID DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Connection c;

    public static void main(String[] args) {
        Map<String, String> options = parseArgs(args);
        UUID userId = options.containsKey("userId") ? UUID.fromString(options.get("userId")) : DEFAULT_USER_ID;

        String url = "jdbc:postgresql://%s:%s/%s?sslmode=disable".formatted(
                requireEnv("POSTGRES_HOST"), requireEnv("POSTGRES_PORT"), requireEnv("POSTGRES_DB"));

        ReportShowcaseSeedCli cli = new ReportShowcaseSeedCli();
        try (Connection connection = DriverManager.getConnection(url, requireEnv("POSTGRES_USER"), requireEnv("POSTGRES_PASSWORD"))) {
            cli.c = connection;
            connection.setAutoCommit(false);

            cli.ensureUser(userId);
            long ready = cli.buildComprehensiveReady(userId);
            long failed = cli.buildFailed(userId);
            long insufficient = cli.buildInsufficient(userId);
            long readyNoGuest = cli.buildReadyZeroGuest(userId);

            connection.commit();

            System.out.println("==================================================");
            System.out.println("userId                       : " + userId);
            System.out.println("READY(모든 카드/지인 변형)    : sessionId=" + ready);
            System.out.println("FAILED                       : sessionId=" + failed);
            System.out.println("INSUFFICIENT_ANALYSIS        : sessionId=" + insufficient);
            System.out.println("READY(지인 0명)              : sessionId=" + readyNoGuest);
            System.out.println("==================================================");
            System.out.println("토큰 발급: ./gradlew issueTestToken -PuserId=" + userId + " -Pprovider=KAKAO");
            System.out.println("조회     : GET /api/v1/interview/sessions/{sessionId}/report");
            System.out.println("==================================================");
        } catch (Exception e) {
            throw new RuntimeException("리포트 쇼케이스 시딩에 실패했습니다.", e);
        }
    }

    // ---------------------------------------------------------------------
    // 세션 A: READY — 모든 카드/하이라이트/레드플래그/해상도/스크립트/지인 변형
    // ---------------------------------------------------------------------
    private long buildComprehensiveReady(UUID userId) throws Exception {
        long s = insertSession(userId, "COMPLETED", "NORMAL_END");
        insertReport(s, "READY", "핵심 경험은 깊이 있게 짚었지만, 규모·갈등 축에서는 근거가 얕았어요.");
        // 영상 메타는 넣되(플레이어 UI 확인용) 합성 전이라 재생 URL은 항상 null이다. wrap-up 멘트가 대본 끝에 붙는다.
        insertVideo(s, false, LocalDateTime.now().plusHours(20), 78.0f, 82.0f);

        float clock = 0f;

        // --- 카드 1: DEPTH d1 — GOOD/SUFFICIENT + IMPROVE/PROBE_WORTHY, scriptSegments 有 ---
        String[] a1 = {
                "처음에는 캐시 TTL을 늘려 조회 부하를 줄였어요. ",
                "그런데 데이터 정합성이 깨져서, 쓰기 시점에 캐시를 무효화하는 방식으로 바꿨습니다. ",
                "결과적으로 P99 지연이 320ms에서 90ms로 줄었어요."};
        long q1 = insertTurn(s, "DEPTH", 1, 1, "캐시 전략을 어떻게 설계했고, 왜 그 방식을 골랐나요?", clock, a1);
        clock += secOf(a1) + 6;
        long c1 = insertCard(s, q1, "DEPTH", 1, "캐시 전략 설계 근거", "어떤 근거로 캐시 방식을 선택하고 개선했는지 묻는 질문입니다.");
        int[][] r1 = ranges(a1);
        insertHighlight(c1, r1[2][0], r1[2][1], "GOOD", "SUFFICIENT", "정량 성과 제시", "원인·조치·결과를 수치로 스스로 짚어 더 물을 지점이 없어요.", null);
        long h1b = insertHighlight(c1, r1[1][0], r1[1][1], "IMPROVE", "PROBE_WORTHY", "무효화 전략의 한계", "정합성 문제를 어떻게 감지했는지 근거가 더 필요해요.", null);
        insertFollowUps(h1b, "정합성이 깨진 것을 어떻게 감지했나요?", "무효화가 실패했을 때 폴백은 무엇이었나요?");

        // --- 카드 2: DEPTH d2 — IMPROVE/OFF_INTENT(의도 대비 3필드) + GOOD/PROBE_WORTHY ---
        String[] a2 = {
                "저는 팀에서 신뢰를 쌓는 걸 가장 중요하게 생각했어요. ",
                "동료와 자주 대화하면서 분위기를 챙겼습니다."};
        long q2 = insertTurn(s, "DEPTH", 2, 2, "장애 상황에서 어떤 근거로 조치 우선순위를 정했나요?", clock, a2);
        clock += secOf(a2) + 6;
        long c2 = insertCard(s, q2, "DEPTH", 2, "장애 대응 의사결정 근거", "장애 상황에서 어떤 근거로 조치 우선순위를 정했는지 묻는 질문입니다.");
        int[][] r2 = ranges(a2);
        insertHighlight(c2, r2[0][0], r2[1][1], "IMPROVE", "OFF_INTENT", "질문 의도와 어긋난 답변",
                "기술적 의사결정 근거를 물었는데 팀 분위기 이야기로 흘렀어요.", "팀 내 신뢰와 분위기");
        long h2b = insertHighlight(c2, r2[1][0], r2[1][1], "GOOD", "PROBE_WORTHY", "협업 태도", "대화로 분위기를 관리한 점은 강점이에요.", null);
        insertFollowUps(h2b, "그 대화에서 구체적으로 어떤 결정을 이끌어냈나요?");

        // --- 카드 3: BOUNDARY d1 — IMPROVE/SHALLOW + 레드플래그 FABRICATION(축) + CONTRADICTION(질문) 겹침 ---
        String[] a3 = {
                "트래픽이 10배로 늘어도 무중단으로 처리했습니다. ",
                "샤딩과 오토스케일링을 직접 설계해서 완벽하게 막았어요."};
        long q3 = insertTurn(s, "BOUNDARY", 1, 3, "예상보다 트래픽이 급증했을 때 어떻게 대응했나요?", clock, a3);
        clock += secOf(a3) + 6;
        long c3 = insertCard(s, q3, "BOUNDARY", 1, "트래픽 급증 대응", "규모가 갑자기 커졌을 때의 대응 설계와 실제 근거를 묻는 질문입니다.");
        int[][] r3 = ranges(a3);
        insertHighlight(c3, r3[1][0], r3[1][1], "IMPROVE", "SHALLOW", "구체성 부족", "'완벽하게'라는 표현만 있고 규모·방법의 근거가 얕아요.", null);

        // --- 카드 4: CONNECTION d1 — 해상도 낮음(FEW_TURNS), 하이라이트 없음, scriptSegments 없음 ---
        long q4 = insertQuestion(s, "CONNECTION", 1, 4, "이 경험을 지원 직무와 어떻게 연결지을 수 있나요?", clock, clock + 3);
        insertAnswer(s, q4, "CONNECTION", "네, 그건 잘 기억이 안 나요.", clock + 3, clock + 6);
        clock += 10;
        insertCard(s, q4, "CONNECTION", 1, "직무 연결", "경험을 지원 직무와 연결짓는 능력을 묻는 질문입니다.");
        // (세그먼트 미삽입 → 이 카드의 scriptSegments는 빈 배열)

        // --- 카드 5: TRADEOFF d1 — 해상도 낮음(SHALLOW_ANSWER), scriptSegments 有 ---
        String[] a5 = {"그냥 되는대로 골랐어요. ", "특별한 기준은 없었습니다."};
        long q5 = insertTurn(s, "TRADEOFF", 1, 5, "여러 대안 중 하나를 고를 때 어떤 기준을 세웠나요?", clock, a5);
        clock += secOf(a5) + 6;
        insertCard(s, q5, "TRADEOFF", 1, "대안 선택 기준", "여러 선택지 사이에서 우선순위를 정하는 기준을 묻는 질문입니다.");

        // --- 카드 6: CONFLICT d1 — 해상도 낮음(OFF_TOPIC) ---
        String[] a6 = {"저는 갈등을 싫어해서 그냥 피했어요."};
        long q6 = insertTurn(s, "CONFLICT", 1, 6, "팀 내 의견 충돌이 있었을 때 어떻게 풀었나요?", clock, a6);
        clock += secOf(a6) + 6;
        insertCard(s, q6, "CONFLICT", 1, "갈등 해결", "의견 충돌 상황에서의 대응 방식을 묻는 질문입니다.");

        // --- 카드 7: RESILIENCE d1 — 레드플래그 PERFECT_NARRATIVE(축) + IMPROVE/SHALLOW ---
        String[] a7 = {"실패한 적은 딱히 없었어요. ", "항상 계획대로 잘 풀렸습니다."};
        long q7 = insertTurn(s, "RESILIENCE", 1, 7, "가장 크게 실패했던 경험과 거기서 배운 점은 무엇인가요?", clock, a7);
        clock += secOf(a7) + 6;
        long c7 = insertCard(s, q7, "RESILIENCE", 1, "실패와 성장", "실패를 인정하고 그로부터 배우는 태도를 묻는 질문입니다.");
        int[][] r7 = ranges(a7);
        insertHighlight(c7, r7[1][0], r7[1][1], "IMPROVE", "SHALLOW", "약점 인정 부재", "어려움이나 실패를 인정한 지점이 거의 없어요.", null);

        // --- 카드 8: DEPTH d3 — 레드플래그 CONTRADICTION(질문 기반) + IMPROVE/PROBE_WORTHY ---
        String[] a8 = {"아까는 혼자 설계했다고 했는데, 사실 팀과 함께 했습니다. ", "제 역할은 리뷰 위주였어요."};
        long q8 = insertTurn(s, "DEPTH", 3, 8, "앞서 말한 설계에서 본인의 실제 역할은 무엇이었나요?", clock, a8);
        clock += secOf(a8) + 6;
        long c8 = insertCard(s, q8, "DEPTH", 3, "실제 기여 범위", "본인이 실제로 담당한 역할의 경계를 묻는 질문입니다.");
        int[][] r8 = ranges(a8);
        long h8 = insertHighlight(c8, r8[1][0], r8[1][1], "IMPROVE", "PROBE_WORTHY", "역할 명확화 필요", "'리뷰 위주'가 구체적으로 어떤 기여였는지 더 확인이 필요해요.", null);
        insertFollowUps(h8, "리뷰에서 어떤 설계 오류를 잡아냈나요?");

        // 레드플래그: 축 기반 2종 + 질문 기반 1종(카드3·카드8에 동시에 걸어 "한 카드 다중 노출"도 시연)
        insertRedFlag(s, "FABRICATION", "BOUNDARY");
        insertRedFlag(s, "PERFECT_NARRATIVE", "RESILIENCE");
        long rfContradiction = insertRedFlag(s, "CONTRADICTION", null);
        insertRedFlagQuestion(rfContradiction, q3);
        insertRedFlagQuestion(rfContradiction, q8);
        // 숨김(비노출) 레드플래그도 하나 심어, 노출 필터가 이 카드에 아무것도 안 붙이는지 확인 가능하게 한다.
        insertRedFlag(s, "BLAME_SHIFTING", "CONFLICT");

        // 축 평가: 해상도 낮음 3종 + 정상 축들
        insertAxisEval(s, "CONNECTION", "LOW", "FEW_TURNS", null);
        insertAxisEval(s, "TRADEOFF", "LOW", "SHALLOW_ANSWER", 40);
        insertAxisEval(s, "CONFLICT", "LOW", "OFF_TOPIC", 35);
        insertAxisEval(s, "DEPTH", "NORMAL", null, 82);
        insertAxisEval(s, "BOUNDARY", "NORMAL", null, 60);
        insertAxisEval(s, "RESILIENCE", "NORMAL", null, 55);

        // 지인 피드백 3명: 5축·레벨 1~4·코멘트 有/無·별칭 null 케이스까지
        long g1 = insertGuest(s, "민지영", "우수한 시선 처리와 목소리");
        insertRating(g1, "GAZE", 4, "시선 처리가 안정적이었어요.");
        insertRating(g1, "EXPRESSION", 3, "표정이 조금 굳어 있었어요.");
        insertRating(g1, "POSTURE", 2, "자세가 자주 흐트러졌어요.");
        insertRating(g1, "GESTURE", 1, null);
        insertRating(g1, "VOICE", 4, "목소리 톤이 아주 좋았어요.");
        long g2 = insertGuest(s, "박현우", null);
        insertRating(g2, "VOICE", 3, "말끝이 흐려질 때가 있었어요.");
        insertRating(g2, "GAZE", 2, "시선이 아래로 향할 때가 많았어요.");
        insertRating(g2, "EXPRESSION", 4, null);
        long g3 = insertGuest(s, null, null); // 별칭(nickname) null → alias null 케이스
        insertRating(g3, "POSTURE", 3, "자세는 무난했어요.");
        insertRating(g3, "GESTURE", 2, null);

        return s;
    }

    // ---------------------------------------------------------------------
    // 상태값 케이스
    // ---------------------------------------------------------------------
    private long buildFailed(UUID userId) throws Exception {
        long s = insertSession(userId, "COMPLETED", null);
        insertReport(s, "FAILED", null);
        return s;
    }

    private long buildInsufficient(UUID userId) throws Exception {
        // INSUFFICIENT_ANALYSIS는 status-only가 아니라 정상 조립 경로를 탄다(카드 없이도 유효). headline만 있는 최소 케이스.
        long s = insertSession(userId, "COMPLETED", null);
        insertReport(s, "INSUFFICIENT_ANALYSIS", "답변이 충분하지 않아 분석을 완료하지 못했어요.");
        return s;
    }

    private long buildReadyZeroGuest(UUID userId) throws Exception {
        long s = insertSession(userId, "COMPLETED", "NORMAL_END");
        insertReport(s, "READY", "짧지만 핵심은 전달된 답변이었어요.");
        String[] a = {"저는 결제 실패율을 로그 기반으로 추적해 원인을 좁혔어요. ", "재시도 큐를 도입해 실패율을 2%에서 0.3%로 낮췄습니다."};
        long q = insertTurn(s, "DEPTH", 1, 1, "장애를 어떻게 추적하고 해결했나요?", 0f, a);
        long card = insertCard(s, q, "DEPTH", 1, "장애 추적·해결", "문제를 어떻게 좁히고 해결했는지 묻는 질문입니다.");
        int[][] r = ranges(a);
        insertHighlight(card, r[1][0], r[1][1], "GOOD", "SUFFICIENT", "정량 개선", "추적→조치→수치 개선까지 스스로 짚었어요.", null);
        // 지인 피드백 없음 → participantCount=0, guests=[]
        return s;
    }

    // ---------------------------------------------------------------------
    // insert helpers
    // ---------------------------------------------------------------------
    private void ensureUser(UUID userId) throws Exception {
        try (PreparedStatement select = c.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
            select.setObject(1, userId);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO users (id, created_at, updated_at, email, name, provider, provider_id, account_status) " +
                        "VALUES (?, ?, ?, ?, ?, 'KAKAO', ?, 'ACTIVE')")) {
            LocalDateTime now = LocalDateTime.now();
            insert.setObject(1, userId);
            insert.setObject(2, now);
            insert.setObject(3, now);
            insert.setString(4, "report-showcase@example.com");
            insert.setString(5, "쇼케이스");
            insert.setString(6, "report-showcase-" + UUID.randomUUID());
            insert.executeUpdate();
        }
    }

    private long insertSession(UUID userId, String status, String endType) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO interview_session (user_id, status, end_type, created_at, started_at, ended_at, " +
                        "snapshot_job_type, snapshot_years_of_experience) VALUES (?, ?, ?, ?, ?, ?, 'BACKEND', 3)",
                Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setObject(1, userId);
            ps.setString(2, status);
            ps.setString(3, endType);
            ps.setObject(4, now);
            ps.setObject(5, now.minusMinutes(20));
            ps.setObject(6, now);
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private void insertReport(long sessionId, String status, String headline) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO report (session_id, status, headline, created_at, composite_score, internal_grade) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, sessionId);
            ps.setString(2, status);
            ps.setString(3, headline);
            ps.setObject(4, LocalDateTime.now());
            ps.setObject(5, 62.5d);
            ps.setString(6, "LEAN");
            ps.executeUpdate();
        }
    }

    private void insertVideo(long sessionId, boolean composited, LocalDateTime expiresAt, Float wrapStart, Float wrapEnd) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO interview_video (session_id, base_at, expires_at, deleted, uploaded, composited, wrap_up_start_sec, wrap_up_end_sec) " +
                        "VALUES (?, ?, ?, false, true, ?, ?, ?)")) {
            ps.setLong(1, sessionId);
            ps.setObject(2, LocalDateTime.now());
            ps.setObject(3, expiresAt);
            ps.setBoolean(4, composited);
            setFloat(ps, 5, wrapStart);
            setFloat(ps, 6, wrapEnd);
            ps.executeUpdate();
        }
    }

    /** 질문 + 답변 + 발화 세그먼트(면접관 1 + 면접자 문장별)를 한 번에 심고 questionId를 반환한다. */
    private long insertTurn(long sessionId, String testType, int depthLevel, int turnLevel,
                            String questionText, float startSec, String[] answerSentences) throws Exception {
        long questionId = insertQuestion(sessionId, testType, depthLevel, turnLevel, questionText, startSec, startSec + 3);
        // 면접관 세그먼트(질문 대본) 1개
        insertSegment(sessionId, questionId, "INTERVIEWER", questionText, 0, questionText.length(), startSec, startSec + 3);
        String transcript = String.join("", answerSentences);
        insertAnswer(sessionId, questionId, testType, transcript, startSec + 3, startSec + 3 + secOf(answerSentences));
        // 면접자 세그먼트: 문장별
        int[][] r = ranges(answerSentences);
        float t = startSec + 3;
        for (int i = 0; i < answerSentences.length; i++) {
            float end = t + 2.5f;
            insertSegment(sessionId, questionId, "INTERVIEWEE", answerSentences[i].strip(), r[i][0], r[i][1], t, end);
            t = end;
        }
        return questionId;
    }

    private long insertQuestion(long sessionId, String testType, int depthLevel, int turnLevel,
                                String content, float startSec, float endSec) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO question (session_id, content, test_type, depth_level, turn_level, is_wrap_up, created_at, " +
                        "question_start_sec, question_end_sec) VALUES (?, ?, ?, ?, ?, false, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sessionId);
            ps.setString(2, content);
            ps.setString(3, testType);
            ps.setInt(4, depthLevel);
            ps.setInt(5, turnLevel);
            ps.setObject(6, LocalDateTime.now());
            ps.setFloat(7, startSec);
            ps.setFloat(8, endSec);
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private void insertAnswer(long sessionId, long questionId, String testType, String sttText, float startSec, float endSec) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO answer (session_id, question_id, test_type, stt_text, is_skipped, ceiling_reached, red_flag_detected, " +
                        "created_at, answer_start_sec, answer_end_sec, answer_duration) VALUES (?, ?, ?, ?, false, false, false, ?, ?, ?, ?)")) {
            ps.setLong(1, sessionId);
            ps.setLong(2, questionId);
            ps.setString(3, testType);
            ps.setString(4, sttText);
            ps.setObject(5, LocalDateTime.now());
            ps.setFloat(6, startSec);
            ps.setFloat(7, endSec);
            ps.setFloat(8, endSec - startSec);
            ps.executeUpdate();
        }
    }

    private void insertSegment(long sessionId, long questionId, String role, String text,
                               int startIndex, int endIndex, float startSec, float endSec) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO utterance_segment (session_id, question_id, role, text, start_index, end_index, start_sec, end_sec) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, sessionId);
            ps.setLong(2, questionId);
            ps.setString(3, role);
            ps.setString(4, text);
            ps.setInt(5, startIndex);
            ps.setInt(6, endIndex);
            ps.setFloat(7, startSec);
            ps.setFloat(8, endSec);
            ps.executeUpdate();
        }
    }

    private long insertCard(long sessionId, long questionId, String testType, int depthLevel,
                            String intentTitle, String intentTranslation) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO report_card (session_id, question_id, test_type, depth_level, question_intent_title, " +
                        "question_intent_translation, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sessionId);
            ps.setLong(2, questionId);
            ps.setString(3, testType);
            ps.setInt(4, depthLevel);
            ps.setString(5, intentTitle);
            ps.setString(6, intentTranslation);
            ps.setObject(7, LocalDateTime.now());
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private long insertHighlight(long cardId, int startIndex, int endIndex, String tone, String reason,
                                 String title, String analysis, String answerTopicTitle) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO report_card_highlight (report_card_id, start_index, end_index, tone, reason, title, analysis, answer_topic_title) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, cardId);
            ps.setInt(2, startIndex);
            ps.setInt(3, endIndex);
            ps.setString(4, tone);
            ps.setString(5, reason);
            ps.setString(6, title);
            ps.setString(7, analysis);
            ps.setString(8, answerTopicTitle);
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private void insertFollowUps(long highlightId, String... questions) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO report_card_highlight_follow_up (report_card_highlight_id, idx, question) VALUES (?, ?, ?)")) {
            for (int i = 0; i < questions.length; i++) {
                ps.setLong(1, highlightId);
                ps.setInt(2, i);
                ps.setString(3, questions[i]);
                ps.executeUpdate();
            }
        }
    }

    private long insertRedFlag(long sessionId, String type, String affectedTestType) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO red_flag (session_id, type, affected_test_type, knockout, cap_value, created_at) " +
                        "VALUES (?, ?, ?, false, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sessionId);
            ps.setString(2, type);
            ps.setString(3, affectedTestType);
            ps.setObject(4, affectedTestType == null ? null : 60, java.sql.Types.INTEGER);
            ps.setObject(5, LocalDateTime.now());
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private void insertRedFlagQuestion(long redFlagId, long questionId) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO red_flag_related_question (red_flag_id, question_id) VALUES (?, ?)")) {
            ps.setLong(1, redFlagId);
            ps.setLong(2, questionId);
            ps.executeUpdate();
        }
    }

    private void insertAxisEval(long sessionId, String testType, String resolutionLevel, String lowReason, Integer score) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO axis_evaluation (session_id, test_type, resolution_level, resolution_low_reason, score, rationale, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, sessionId);
            ps.setString(2, testType);
            ps.setString(3, resolutionLevel);
            ps.setString(4, lowReason);
            ps.setObject(5, score, java.sql.Types.INTEGER);
            ps.setString(6, "쇼케이스 시딩 데이터");
            ps.setObject(7, LocalDateTime.now());
            ps.executeUpdate();
        }
    }

    private long insertGuest(long sessionId, String nickname, String overall) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO guest_feedback (session_id, device_id, nickname, overall_feedback, submitted_at) " +
                        "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sessionId);
            ps.setString(2, "device-" + UUID.randomUUID());
            ps.setString(3, nickname);
            ps.setString(4, overall);
            ps.setObject(5, LocalDateTime.now());
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private void insertRating(long guestId, String axis, int level, String comment) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO guest_feedback_rating (guest_feedback_id, axis, level, comment) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, guestId);
            ps.setString(2, axis);
            ps.setInt(3, level);
            ps.setString(4, comment);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------------
    // util
    // ---------------------------------------------------------------------
    // 문장 배열을 이어붙였을 때 각 문장의 [시작, 끝) 문자 인덱스를 계산한다(하이라이트·세그먼트가 같은 좌표계를 쓰도록).
    private static int[][] ranges(String[] sentences) {
        int[][] r = new int[sentences.length][2];
        int offset = 0;
        for (int i = 0; i < sentences.length; i++) {
            r[i][0] = offset;
            offset += sentences[i].length();
            r[i][1] = offset;
        }
        return r;
    }

    private static float secOf(String[] sentences) {
        return sentences.length * 2.5f;
    }

    private static void setFloat(PreparedStatement ps, int idx, Float value) throws Exception {
        if (value == null) {
            ps.setNull(idx, java.sql.Types.REAL);
        } else {
            ps.setFloat(idx, value);
        }
    }

    private static long generatedKey(PreparedStatement ps) throws Exception {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getLong(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            String[] parts = arg.split("=", 2);
            if (parts.length == 2 && parts[0].startsWith("--")) {
                options.put(parts[0].substring(2), parts[1]);
            }
        }
        return options;
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " 환경변수가 설정되어 있지 않습니다.");
        }
        return value;
    }
}
