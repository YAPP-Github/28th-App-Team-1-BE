package com.yapp.d14.devtool.tool;

import com.yapp.d14.D14Application;
import com.yapp.d14.devtool.tool.DummyAnswerAudioGenerator.AnswerAudio;
import com.yapp.d14.feedback.application.command.FeedbackShareCreateCommand;
import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand;
import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand.RawRating;
import com.yapp.d14.feedback.application.port.in.FeedbackShareCreateUseCase;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackSubmitUseCase;
import com.yapp.d14.feedback.application.port.in.result.FeedbackShareCreateResult;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackSubmitResult;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import com.yapp.d14.interview.application.command.InterviewVideoUploadCompleteCommand;
import com.yapp.d14.interview.application.port.in.AudioStreamUseCase;
import com.yapp.d14.interview.application.port.in.InterviewAnswerSubmitUseCase;
import com.yapp.d14.interview.application.port.in.InterviewReportQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionPreloadUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionStatusUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoQueryUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadCompleteUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoUploadUrlIssueUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.in.result.InterviewReportQueryResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionPollStatus;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoPlaybackResult;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;
import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.PortfolioListUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioListResult;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 특정 userId에 대해 완료된 면접 세션 → 생성된 리포트 → 지인 피드백(+영상)까지
 * 실제 애플리케이션 파이프라인을 그대로 호출해 만든다. 실제 Anthropic/OpenAI/ffmpeg 호출이 발생하므로
 * 로컬 Postgres/Redis, ANTHROPIC_API_KEY, OPENAI_API_KEY, AWS S3 자격증명, ffmpeg가 필요하다.
 * ./gradlew seedInterviewFullPipeline [-PuserId=<uuid>] [-PuserName=<name>] [-PturnCount=3]
 */
public class InterviewFullPipelineSeedCli {

    private static final String DEFAULT_USER_NAME = "더미유저";
    private static final int DEFAULT_TURN_COUNT = 3;

    private static final String JD_TEXT = """
            [백엔드 엔지니어 채용]
            - Spring Boot 기반 백엔드 서비스 설계 및 운영 경험
            - RESTful API 설계, 데이터베이스 스키마 설계·운영 경험
            - 대용량 트래픽 환경에서의 성능 최적화 경험 우대
            - PostgreSQL, Redis 등 데이터 저장소 운영 경험 우대
            """;

    private static final String FREE_TEXT = """
            3년차 백엔드 엔지니어입니다. Spring Boot로 API 서버를 설계·운영해왔고,
            트래픽 증가에 따른 성능 이슈를 데이터베이스 인덱스 튜닝과 캐싱으로 해결한 경험이 있습니다.
            """;

    private static final float QUESTION_AUDIO_DURATION_SEC = 4f;
    private static final String MANUAL_END_TYPE = "MANUAL_END";

    public static void main(String[] args) {
        Map<String, String> options = parseArgs(args);
        UUID userId = options.containsKey("userId")
                ? UUID.fromString(options.get("userId"))
                : UUID.randomUUID();
        String userName = options.getOrDefault("userName", DEFAULT_USER_NAME);
        int turnCount = options.containsKey("turnCount")
                ? Integer.parseInt(options.get("turnCount"))
                : DEFAULT_TURN_COUNT;

        ensureUser(userId, userName);

        ConfigurableApplicationContext context = new SpringApplicationBuilder(D14Application.class)
                .web(WebApplicationType.NONE)
                .run();

        try {
            UUID portfolioId = resolvePortfolioId(
                    userId, context.getBean(PortfolioListUseCase.class), context.getBean(PortfolioRegisterUseCase.class)
            );
            awaitPortfolioReady(userId, portfolioId, context.getBean(PortfolioStatusUseCase.class));

            Long sessionId = createSession(userId, portfolioId, context.getBean(InterviewSessionCreateUseCase.class));
            InterviewSessionStatusResult.SummaryQuestion summaryQuestion = preloadAndAwaitReady(
                    userId, sessionId,
                    context.getBean(InterviewSessionPreloadUseCase.class),
                    context.getBean(InterviewSessionStatusUseCase.class)
            );

            float totalTimelineSec = runAnswerTurns(
                    userId, sessionId, summaryQuestion, turnCount,
                    context.getBean(AudioStreamUseCase.class),
                    context.getBean(TextToSpeechSynthesizer.class),
                    context.getBean(InterviewAnswerSubmitUseCase.class)
            );

            ReportStatus reportStatus = awaitReportReady(userId, sessionId, context.getBean(InterviewReportQueryUseCase.class));

            String playbackUrl = uploadAndCompositeVideo(
                    userId, sessionId, totalTimelineSec,
                    context.getBean(InterviewVideoUploadUrlIssueUseCase.class),
                    context.getBean(InterviewVideoUploadCompleteUseCase.class),
                    context.getBean(InterviewVideoQueryUseCase.class)
            );

            Long feedbackSubmissionId = submitGuestFeedback(
                    userId, sessionId,
                    context.getBean(FeedbackShareCreateUseCase.class),
                    context.getBean(GuestFeedbackSubmitUseCase.class)
            );

            System.out.println("==============================================");
            System.out.println("userId              : " + userId);
            System.out.println("userName            : " + userName);
            System.out.println("turnCount           : " + turnCount);
            System.out.println("portfolioId         : " + portfolioId);
            System.out.println("sessionId           : " + sessionId);
            System.out.println("totalTimelineSec    : " + totalTimelineSec);
            System.out.println("reportStatus        : " + reportStatus);
            System.out.println("playbackUrl         : " + playbackUrl);
            System.out.println("feedbackSubmissionId: " + feedbackSubmissionId);
            System.out.println("==============================================");
        } finally {
            SpringApplication.exit(context);
        }
    }

    private static UUID resolvePortfolioId(
            UUID userId,
            PortfolioListUseCase portfolioListUseCase,
            PortfolioRegisterUseCase portfolioRegisterUseCase
    ) {
        PortfolioListResult existing = portfolioListUseCase.getList(userId);
        if (!existing.portfolios().isEmpty()) {
            PortfolioSummary summary = existing.portfolios().get(0);
            System.out.println("[PORTFOLIO] 기존 포트폴리오 재사용: portfolioId=" + summary.portfolioId() + ", status=" + summary.status());
            return summary.portfolioId();
        }
        return registerPortfolio(userId, portfolioRegisterUseCase);
    }

    private static UUID registerPortfolio(UUID userId, PortfolioRegisterUseCase portfolioRegisterUseCase) {
        byte[] fileContent = DummyPdfGenerator.generate();
        PortfolioRegisterCommand command = new PortfolioRegisterCommand(
                userId, fileContent, "dummy-resume.pdf", fileContent.length, 1, "application/pdf"
        );
        PortfolioRegisterResult result = portfolioRegisterUseCase.register(command);
        System.out.println("[PORTFOLIO] 등록 완료: portfolioId=" + result.portfolioId() + ", status=" + result.status());
        return result.portfolioId();
    }

    private static void awaitPortfolioReady(UUID userId, UUID portfolioId, PortfolioStatusUseCase portfolioStatusUseCase) {
        Instant deadline = Instant.now().plusSeconds(60);
        while (Instant.now().isBefore(deadline)) {
            PortfolioStatusResult status = portfolioStatusUseCase.getStatus(userId, portfolioId);
            if (status.status() == PortfolioStatus.READY) {
                System.out.println("[PORTFOLIO] 처리 완료: portfolioId=" + portfolioId);
                return;
            }
            if (status.status() == PortfolioStatus.FAILED_FILE || status.status() == PortfolioStatus.FAILED_SYSTEM) {
                throw new IllegalStateException(
                        "포트폴리오 처리 실패: portfolioId=" + portfolioId + ", status=" + status.status() + ", message=" + status.message()
                );
            }
            sleep(2000);
        }
        throw new IllegalStateException("포트폴리오 처리 대기 시간 초과: portfolioId=" + portfolioId);
    }

    private static Long createSession(UUID userId, UUID portfolioId, InterviewSessionCreateUseCase interviewSessionCreateUseCase) {
        InterviewSessionCreateCommand command = new InterviewSessionCreateCommand(userId, portfolioId, null, JD_TEXT, FREE_TEXT);
        InterviewSessionCreateResult result = interviewSessionCreateUseCase.create(command);
        System.out.println("[SESSION] 생성 완료: sessionId=" + result.sessionId() + ", status=" + result.status());
        return result.sessionId();
    }

    private static InterviewSessionStatusResult.SummaryQuestion preloadAndAwaitReady(
            UUID userId,
            Long sessionId,
            InterviewSessionPreloadUseCase interviewSessionPreloadUseCase,
            InterviewSessionStatusUseCase interviewSessionStatusUseCase
    ) {
        interviewSessionPreloadUseCase.preload(sessionId);

        Instant deadline = Instant.now().plusSeconds(90);
        while (Instant.now().isBefore(deadline)) {
            InterviewSessionStatusResult status = interviewSessionStatusUseCase.getStatus(userId, sessionId);
            if (status.status() == InterviewSessionPollStatus.READY) {
                System.out.println("[SESSION] preload 완료: sessionId=" + sessionId + ", questionId=" + status.summaryQuestion().questionId());
                return status.summaryQuestion();
            }
            if (status.status() == InterviewSessionPollStatus.FAILED) {
                throw new IllegalStateException("세션 preload 실패: sessionId=" + sessionId);
            }
            sleep(2000);
        }
        throw new IllegalStateException("세션 preload 대기 시간 초과: sessionId=" + sessionId);
    }

    private static float runAnswerTurns(
            UUID userId,
            Long sessionId,
            InterviewSessionStatusResult.SummaryQuestion summaryQuestion,
            int turnCount,
            AudioStreamUseCase audioStreamUseCase,
            TextToSpeechSynthesizer textToSpeechSynthesizer,
            InterviewAnswerSubmitUseCase interviewAnswerSubmitUseCase
    ) {
        Long questionId = summaryQuestion.questionId();
        int turnLevel = summaryQuestion.turnLevel();
        float timelineCursorSec = 0f;

        for (int turnIndex = 0; turnIndex < turnCount; turnIndex++) {
            if (turnLevel >= 1) {
                audioStreamUseCase.stream(userId, sessionId, questionId).collectList().block();
            }
            float questionAudioStartSec = timelineCursorSec;
            timelineCursorSec += QUESTION_AUDIO_DURATION_SEC;
            float questionAudioEndSec = timelineCursorSec;

            AnswerAudio answerAudio = DummyAnswerAudioGenerator.synthesizeAnswerAudio(textToSpeechSynthesizer, turnIndex);
            float answerStartSec = timelineCursorSec;
            timelineCursorSec += answerAudio.durationSec();
            float answerEndSec = timelineCursorSec;

            boolean isLastTurn = turnIndex == turnCount - 1;
            String endType = isLastTurn ? MANUAL_END_TYPE : null;

            InterviewAnswerSubmitCommand command = InterviewAnswerSubmitCommand.of(
                    sessionId, questionId, answerAudio.content(),
                    questionAudioStartSec, questionAudioEndSec,
                    answerStartSec, answerEndSec, answerAudio.durationSec(),
                    endType, null
            );
            InterviewAnswerSubmitResult result = interviewAnswerSubmitUseCase.submit(userId, command);
            System.out.println("[ANSWER] turn=" + turnIndex + ", questionId=" + questionId
                    + ", sessionEnded=" + result.sessionEnded() + ", endType=" + result.endType());

            if (result.nextQuestion() != null) {
                questionId = result.nextQuestion().questionId();
                turnLevel = result.nextQuestion().turnLevel();
            }
        }

        return timelineCursorSec;
    }

    private static ReportStatus awaitReportReady(UUID userId, Long sessionId, InterviewReportQueryUseCase interviewReportQueryUseCase) {
        Instant deadline = Instant.now().plusSeconds(180);
        while (Instant.now().isBefore(deadline)) {
            InterviewReportQueryResult report = interviewReportQueryUseCase.getReport(userId, sessionId);
            if (report.status() == ReportStatus.READY
                    || report.status() == ReportStatus.INSUFFICIENT_ANALYSIS
                    || report.status() == ReportStatus.FAILED) {
                System.out.println("[REPORT] 생성 완료: sessionId=" + sessionId + ", status=" + report.status());
                return report.status();
            }
            sleep(3000);
        }
        throw new IllegalStateException("리포트 생성 대기 시간 초과: sessionId=" + sessionId);
    }

    private static String uploadAndCompositeVideo(
            UUID userId,
            Long sessionId,
            float totalTimelineSec,
            InterviewVideoUploadUrlIssueUseCase interviewVideoUploadUrlIssueUseCase,
            InterviewVideoUploadCompleteUseCase interviewVideoUploadCompleteUseCase,
            InterviewVideoQueryUseCase interviewVideoQueryUseCase
    ) {
        InterviewVideoUploadUrlResult uploadUrlResult = interviewVideoUploadUrlIssueUseCase.issue(userId, sessionId);

        Path rawVideoFile = DummyRawVideoGenerator.generate(totalTimelineSec);
        try {
            putToPresignedUrl(uploadUrlResult.uploadUrl(), uploadUrlResult.contentType(), rawVideoFile);
        } finally {
            deleteQuietly(rawVideoFile);
        }

        interviewVideoUploadCompleteUseCase.complete(new InterviewVideoUploadCompleteCommand(userId, sessionId, null, null));
        System.out.println("[VIDEO] 업로드 완료 처리, 합성 대기 시작: sessionId=" + sessionId);

        return awaitCompositeReady(sessionId, interviewVideoQueryUseCase);
    }

    private static void putToPresignedUrl(String uploadUrl, String contentType, Path file) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofFile(file))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("presigned URL 업로드 실패: status=" + response.statusCode());
            }
            System.out.println("[VIDEO] 원본 영상 업로드 완료: status=" + response.statusCode());
        } catch (IOException e) {
            throw new UncheckedIOException("원본 영상 업로드 중 IO 오류가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("원본 영상 업로드가 중단되었습니다.", e);
        }
    }

    private static String awaitCompositeReady(Long sessionId, InterviewVideoQueryUseCase interviewVideoQueryUseCase) {
        Instant deadline = Instant.now().plusSeconds(120);
        while (Instant.now().isBefore(deadline)) {
            InterviewVideoPlaybackResult playback = interviewVideoQueryUseCase.getPlayback(sessionId);
            if (playback.playbackUrl() != null) {
                System.out.println("[VIDEO] 합성 완료: sessionId=" + sessionId);
                return playback.playbackUrl();
            }
            sleep(3000);
        }
        throw new IllegalStateException("영상 합성 대기 시간 초과: sessionId=" + sessionId);
    }

    private static Long submitGuestFeedback(
            UUID userId,
            Long sessionId,
            FeedbackShareCreateUseCase feedbackShareCreateUseCase,
            GuestFeedbackSubmitUseCase guestFeedbackSubmitUseCase
    ) {
        FeedbackShareCreateCommand shareCommand = new FeedbackShareCreateCommand(userId, sessionId, List.of(AttitudeAxis.values()));
        FeedbackShareCreateResult shareResult = feedbackShareCreateUseCase.create(shareCommand);
        System.out.println("[FEEDBACK] 공유 링크 발급 완료: token=" + shareResult.token());

        List<RawRating> rawRatings = Arrays.stream(AttitudeAxis.values())
                .map(axis -> new RawRating(axis.name(), 3, "더미 피드백입니다."))
                .toList();
        String deviceId = "dummy-device-" + UUID.randomUUID();
        GuestFeedbackSubmitCommand submitCommand = GuestFeedbackSubmitCommand.of(shareResult.token(), deviceId, "테스트지인", rawRatings);
        GuestFeedbackSubmitResult submitResult = guestFeedbackSubmitUseCase.submit(submitCommand);
        System.out.println("[FEEDBACK] 게스트 피드백 제출 완료: submissionId=" + submitResult.submissionId());
        return submitResult.submissionId();
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 임시 파일 정리 실패는 CLI 결과에 영향을 주지 않는다.
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void ensureUser(UUID userId, String userName) {
        String url = "jdbc:postgresql://%s:%s/%s?sslmode=disable".formatted(
                requireEnv("POSTGRES_HOST"), requireEnv("POSTGRES_PORT"), requireEnv("POSTGRES_DB")
        );

        try (Connection connection = DriverManager.getConnection(url, requireEnv("POSTGRES_USER"), requireEnv("POSTGRES_PASSWORD"))) {
            connection.setAutoCommit(false);

            try (PreparedStatement select = connection.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
                select.setObject(1, userId);
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        connection.commit();
                        return;
                    }
                }
            }

            String providerId = "interview-full-pipeline-fixture-" + UUID.randomUUID();
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users (id, created_at, updated_at, email, name, provider, provider_id) " +
                            "VALUES (?, ?, ?, ?, ?, 'KAKAO', ?)"
            )) {
                LocalDateTime now = LocalDateTime.now();
                insert.setObject(1, userId);
                insert.setObject(2, now);
                insert.setObject(3, now);
                insert.setString(4, "interview-full-pipeline-fixture@example.com");
                insert.setString(5, userName);
                insert.setString(6, providerId);
                insert.executeUpdate();
            }

            connection.commit();
        } catch (Exception e) {
            throw new RuntimeException("유저 픽스처 생성에 실패했습니다.", e);
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
