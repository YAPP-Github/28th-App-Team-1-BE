# syntax=docker/dockerfile:1.6

FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

COPY src src

RUN ./gradlew bootJar --no-daemon -x test


FROM eclipse-temurin:21-jre-jammy AS runtime

# 면접 영상·질문 음성 합성(FfmpegInterviewVideoCompositorAdapter)이 ffmpeg 바이너리를 서브프로세스로 호출한다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx400m", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Duser.timezone=Asia/Seoul", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
