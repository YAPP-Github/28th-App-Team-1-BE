package com.yapp.d14.feedback.tool;

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
 * F4·G4 API를 호출해볼 수 있는 최소 상태(완료된 면접 세션 + 영상 메타데이터)를 DB에 직접 심는다.
 * 실제 면접 진행·리포트 생성 파이프라인을 거치지 않고 프론트 개발용으로 즉시 사용할 수 있게 하기 위함이다.
 * ./gradlew seedFeedbackFixture [-PuserId=<uuid>] [-PuserName=<name>]
 */
public class FeedbackFixtureSeedCli {

    private static final String DEFAULT_USER_NAME = "재원";

    public static void main(String[] args) {
        Map<String, String> options = parseArgs(args);
        UUID userId = options.containsKey("userId")
                ? UUID.fromString(options.get("userId"))
                : UUID.randomUUID();
        String userName = options.getOrDefault("userName", DEFAULT_USER_NAME);

        String url = "jdbc:postgresql://%s:%s/%s?sslmode=disable".formatted(
                requireEnv("POSTGRES_HOST"), requireEnv("POSTGRES_PORT"), requireEnv("POSTGRES_DB")
        );

        try (Connection connection = DriverManager.getConnection(url, requireEnv("POSTGRES_USER"), requireEnv("POSTGRES_PASSWORD"))) {
            connection.setAutoCommit(false);

            ensureUser(connection, userId, userName);
            long sessionId = insertCompletedSession(connection, userId);
            insertVideo(connection, sessionId);

            connection.commit();

            System.out.println("==============================================");
            System.out.println("userId    : " + userId);
            System.out.println("sessionId : " + sessionId);
            System.out.println("==============================================");
            System.out.println("다음 명령으로 이 유저의 액세스 토큰을 발급하세요:");
            System.out.println("  ./gradlew issueTestToken -PuserId=" + userId + " -Pprovider=KAKAO");
            System.out.println("F4 생성:");
            System.out.println("  POST /api/v1/feedback/sessions/" + sessionId + "/share");
            System.out.println("==============================================");
        } catch (Exception e) {
            throw new RuntimeException("피드백 픽스처 시딩에 실패했습니다.", e);
        }
    }

    private static void ensureUser(Connection connection, UUID userId, String userName) throws Exception {
        try (PreparedStatement select = connection.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
            select.setObject(1, userId);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        String providerId = "feedback-fixture-" + UUID.randomUUID();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users (id, created_at, updated_at, email, name, provider, provider_id) " +
                        "VALUES (?, ?, ?, ?, ?, 'KAKAO', ?)"
        )) {
            LocalDateTime now = LocalDateTime.now();
            insert.setObject(1, userId);
            insert.setObject(2, now);
            insert.setObject(3, now);
            insert.setString(4, "feedback-fixture@example.com");
            insert.setString(5, userName);
            insert.setString(6, providerId);
            insert.executeUpdate();
        }
    }

    private static long insertCompletedSession(Connection connection, UUID userId) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO interview_session (user_id, status) VALUES (?, 'COMPLETED')",
                Statement.RETURN_GENERATED_KEYS
        )) {
            insert.setObject(1, userId);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void insertVideo(Connection connection, long sessionId) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO interview_video (session_id, base_at, expires_at, deleted) VALUES (?, ?, ?, false)"
        )) {
            LocalDateTime baseAt = LocalDateTime.now();
            insert.setLong(1, sessionId);
            insert.setObject(2, baseAt);
            insert.setObject(3, baseAt.plusHours(24));
            insert.executeUpdate();
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
