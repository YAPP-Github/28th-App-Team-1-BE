package com.yapp.d14.interview.adapter.out.integration.media;

import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 서브프로세스로 녹화본과 질문 TTS를 합성한다. ffmpeg 바이너리는 PATH에서 해석되므로
 * 실행 환경(배포 이미지)에 반드시 설치돼 있어야 한다. S3 입출력·임시파일 수명주기를 모두 이 어댑터가 캡슐화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FfmpegInterviewVideoCompositorAdapter implements InterviewVideoCompositor {

    private static final String FFMPEG_BINARY = "ffmpeg";
    private static final String CONTENT_TYPE = "video/mp4";
    // 합성이 오래 걸려도 스레드를 무한 점유하지 않도록 상한을 둔다. 초과 시 프로세스를 강제 종료하고 실패 처리한다.
    private static final long PROCESS_TIMEOUT_SECONDS = 600L;

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public void compose(UUID userId, Long sessionId, List<QuestionAudioTrack> tracks) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("composite-%s-".formatted(sessionId));

            Path rawVideo = workDir.resolve("raw.mp4");
            download(S3KeyGenerator.interviewRecordingKey(userId, sessionId), rawVideo);

            List<Path> audioFiles = new ArrayList<>();
            for (int i = 0; i < tracks.size(); i++) {
                Path audio = workDir.resolve("q%d.mp3".formatted(i));
                download(tracks.get(i).audioS3Key(), audio);
                audioFiles.add(audio);
            }

            Path output = workDir.resolve("final.mp4");
            runFfmpeg(rawVideo, audioFiles, tracks, output, workDir.resolve("ffmpeg.log"));

            upload(S3KeyGenerator.interviewCompositeKey(userId, sessionId), output);
        } catch (IOException e) {
            throw new UncheckedIOException("[COMPOSITE] 임시파일 처리 실패: sessionId=%d".formatted(sessionId), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("[COMPOSITE] 합성이 중단됨: sessionId=%d".formatted(sessionId), e);
        } finally {
            cleanup(workDir);
        }
    }

    private void runFfmpeg(Path video, List<Path> audios, List<QuestionAudioTrack> tracks, Path output, Path ffmpegLog)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(FFMPEG_BINARY);
        command.add("-y");
        command.add("-i");
        command.add(video.toString());
        for (Path audio : audios) {
            command.add("-i");
            command.add(audio.toString());
        }

        // 각 질문 오디오(입력 1..N)를 시작초만큼 지연(adelay, ms) → 면접자 오디오([0:a])와 함께 amix.
        // normalize=0: 입력 수에 따라 볼륨이 줄지 않도록(면접자 음성이 작아지는 것 방지).
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < audios.size(); i++) {
            long delayMs = Math.max(0L, Math.round(tracks.get(i).startSec() * 1000.0));
            filter.append("[%d:a]adelay=%d:all=1[qa%d];".formatted(i + 1, delayMs, i));
        }
        filter.append("[0:a]");
        for (int i = 0; i < audios.size(); i++) {
            filter.append("[qa%d]".formatted(i));
        }
        filter.append("amix=inputs=%d:normalize=0[aout]".formatted(audios.size() + 1));

        command.add("-filter_complex");
        command.add(filter.toString());
        command.add("-map");
        command.add("0:v");
        command.add("-map");
        command.add("[aout]");
        command.add("-c:v");
        command.add("copy");
        command.add("-c:a");
        command.add("aac");
        command.add("-shortest");
        command.add(output.toString());

        // 파이프 대신 로그 파일로 리다이렉트해 stdout/stderr 버퍼 고갈로 인한 교착을 피한다.
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ffmpegLog.toFile())
                .start();

        boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("[COMPOSITE] ffmpeg 타임아웃(%ds 초과)".formatted(PROCESS_TIMEOUT_SECONDS));
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(
                    "[COMPOSITE] ffmpeg 종료코드 %d, log:%n%s".formatted(process.exitValue(), tail(ffmpegLog)));
        }
    }

    private void download(String key, Path destination) {
        s3Client.getObject(GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build(), destination);
    }

    private void upload(String key, Path source) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(CONTENT_TYPE)
                .build(), source);
    }

    private String tail(Path ffmpegLog) {
        try {
            List<String> lines = Files.readAllLines(ffmpegLog);
            int from = Math.max(0, lines.size() - 20);
            return String.join(System.lineSeparator(), lines.subList(from, lines.size()));
        } catch (IOException e) {
            return "(로그 읽기 실패: %s)".formatted(e.getMessage());
        }
    }

    private void cleanup(Path workDir) {
        if (workDir == null) {
            return;
        }
        try (var paths = Files.walk(workDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("[COMPOSITE] 임시파일 삭제 실패: {}", path, e);
                }
            });
        } catch (IOException e) {
            log.warn("[COMPOSITE] 임시 디렉토리 정리 실패: {}", workDir, e);
        }
    }
}
