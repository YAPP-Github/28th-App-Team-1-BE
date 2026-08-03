package com.yapp.d14.devtool.tool;

import com.yapp.d14.D14Application;
import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
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
            UUID portfolioId = registerPortfolio(userId, context.getBean(PortfolioRegisterUseCase.class));
            awaitPortfolioReady(userId, portfolioId, context.getBean(PortfolioStatusUseCase.class));

            System.out.println("==============================================");
            System.out.println("userId      : " + userId);
            System.out.println("userName    : " + userName);
            System.out.println("turnCount   : " + turnCount);
            System.out.println("portfolioId : " + portfolioId);
            System.out.println("이후 단계는 순차적으로 추가될 예정입니다.");
            System.out.println("==============================================");
        } finally {
            SpringApplication.exit(context);
        }
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
