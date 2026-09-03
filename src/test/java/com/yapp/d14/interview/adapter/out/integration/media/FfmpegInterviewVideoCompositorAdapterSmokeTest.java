package com.yapp.d14.interview.adapter.out.integration.media;

import com.yapp.d14.common.metrics.VideoCompositeMetrics;
import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor.AudioTrack;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 로컬 ffmpeg로 실제 합성이 되는지 확인하는 스모크 테스트.
 * S3Client만 로컬 파일 복사로 대체하고, 필터 구성·ffmpeg 실행·매핑은 프로덕션 어댑터를 그대로 탄다.
 * ffmpeg/ffprobe가 PATH에 있어야 하며, 결과물은 build/composite-smoke/final.mp4로 남으니 직접 재생해 확인할 수 있다.
 * CI(./gradlew test)에서는 제외되고 `./gradlew compositeSmokeTest`로만 실행된다.
 */
@Tag("composite-smoke")
class FfmpegInterviewVideoCompositorAdapterSmokeTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final Long SESSION_ID = 4242L;
    private static final String QUESTION_KEY = "q-key";
    private static final String ANSWER_KEY = "a-key";

    private static Path outputDir;
    private static Path rawVideo;
    private static Path questionAudio;
    private static Path answerAudio;

    @BeforeAll
    static void generateSampleInputs() throws Exception {
        assumeThat(commandAvailable("ffmpeg")).as("ffmpeg 미설치 — brew install ffmpeg").isTrue();
        assumeThat(commandAvailable("ffprobe")).as("ffprobe 미설치").isTrue();

        outputDir = Path.of("build", "composite-smoke");
        Path inputsDir = outputDir.resolve("inputs");
        Files.createDirectories(inputsDir);

        rawVideo = inputsDir.resolve("raw.mp4");
        questionAudio = inputsDir.resolve("q.mp3");
        answerAudio = inputsDir.resolve("a.m4a");

        // 10초 무음 테스트 영상(녹화본 대역). 오디오 트랙 없음 — 서버가 얹는 오디오만으로 합성되는지 본다.
        run("ffmpeg", "-y", "-f", "lavfi", "-i", "testsrc=duration=10:size=320x240:rate=30",
                "-pix_fmt", "yuv420p", rawVideo.toString());
        // 질문 TTS 대역: 440Hz 2초.
        run("ffmpeg", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=2", questionAudio.toString());
        // 답변 음성 대역: 880Hz 3초, m4a/aac(실제 답변 저장 포맷과 동일).
        run("ffmpeg", "-y", "-f", "lavfi", "-i", "sine=frequency=880:duration=3", "-c:a", "aac", answerAudio.toString());
    }

    @Test
    void 질문TTS와_답변음성을_시작초만큼_지연해_합성하고_영상은_원본_길이를_유지한다() throws Exception {
        Map<String, Path> keyToSample = new HashMap<>();
        keyToSample.put(S3KeyGenerator.interviewRecordingKey(USER_ID, SESSION_ID), rawVideo);
        keyToSample.put(QUESTION_KEY, questionAudio);
        keyToSample.put(ANSWER_KEY, answerAudio);

        Path finalOutput = outputDir.resolve("final.mp4");
        Files.deleteIfExists(finalOutput);

        S3Client s3Client = mock(S3Client.class);
        // getObject(req, Path): 요청 key에 매핑된 로컬 샘플을 목적지로 복사(S3 다운로드 대역).
        given(s3Client.getObject(any(GetObjectRequest.class), any(Path.class))).willAnswer(invocation -> {
            GetObjectRequest request = invocation.getArgument(0);
            Path destination = invocation.getArgument(1);
            Path sample = keyToSample.get(request.key());
            assertThat(sample).as("매핑되지 않은 key: %s", request.key()).isNotNull();
            Files.copy(sample, destination, StandardCopyOption.REPLACE_EXISTING);
            return GetObjectResponse.builder().build();
        });
        // putObject(req, Path): 합성 산출물을 재생 가능한 위치로 복사(S3 업로드 대역).
        given(s3Client.putObject(any(PutObjectRequest.class), any(Path.class))).willAnswer(invocation -> {
            Path source = invocation.getArgument(1);
            Files.copy(source, finalOutput, StandardCopyOption.REPLACE_EXISTING);
            return PutObjectResponse.builder().build();
        });

        S3Properties s3Properties = new S3Properties();
        s3Properties.setBucket("smoke-test-bucket");

        FfmpegInterviewVideoCompositorAdapter adapter =
                new FfmpegInterviewVideoCompositorAdapter(s3Client, s3Properties,
                        new VideoCompositeMetrics(new SimpleMeterRegistry()));

        // 질문 1s, 답변 4s 지점에서 시작(녹화 타임라인). 두 트랙을 adelay 후 amix.
        adapter.compose(USER_ID, SESSION_ID, List.of(
                new AudioTrack(QUESTION_KEY, 1.0f),
                new AudioTrack(ANSWER_KEY, 4.0f)
        ));

        assertThat(Files.exists(finalOutput)).as("합성 결과물 생성").isTrue();
        assertThat(Files.size(finalOutput)).isPositive();

        // 영상 + 오디오 스트림이 모두 있어야 한다.
        String codecTypes = run("ffprobe", "-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=codec_type", "-of", "default=nw=1:nk=1", finalOutput.toString());
        assertThat(codecTypes.trim()).isEqualTo("video");
        String audioType = run("ffprobe", "-v", "error", "-select_streams", "a:0",
                "-show_entries", "stream=codec_type", "-of", "default=nw=1:nk=1", finalOutput.toString());
        assertThat(audioType.trim()).isEqualTo("audio");

        // -shortest를 쓰지 않으므로, 합성 오디오(최대 7s)가 영상(10s)보다 짧아도 영상은 원본 길이를 유지해야 한다.
        double duration = Double.parseDouble(run("ffprobe", "-v", "error",
                "-show_entries", "format=duration", "-of", "default=nw=1:nk=1", finalOutput.toString()).trim());
        assertThat(duration).as("영상 길이(원본 10s 유지)").isBetween(9.5, 10.8);

        System.out.println("[composite-smoke] 합성 결과물: " + finalOutput.toAbsolutePath() + " — 재생해서 확인하세요");
    }

    private static boolean commandAvailable(String command) {
        try {
            new ProcessBuilder(command, "-version").start().waitFor(10, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 외부 프로세스를 실행하고 stdout을 반환한다. 비정상 종료 시 stderr를 포함해 예외를 던진다.
    private static String run(String... command) throws Exception {
        Path stdout = Files.createTempFile("smoke-out", ".txt");
        Path stderr = Files.createTempFile("smoke-err", ".txt");
        try {
            Process process = new ProcessBuilder(command)
                    .redirectOutput(stdout.toFile())
                    .redirectError(stderr.toFile())
                    .start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("명령 타임아웃: " + String.join(" ", command));
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("명령 실패(%d): %s%n%s"
                        .formatted(process.exitValue(), String.join(" ", command), Files.readString(stderr)));
            }
            return Files.readString(stdout);
        } finally {
            Files.deleteIfExists(stdout);
            Files.deleteIfExists(stderr);
        }
    }
}
