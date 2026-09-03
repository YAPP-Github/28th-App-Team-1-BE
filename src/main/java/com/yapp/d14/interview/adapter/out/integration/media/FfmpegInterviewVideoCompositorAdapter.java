package com.yapp.d14.interview.adapter.out.integration.media;

import com.yapp.d14.common.metrics.VideoCompositeMetrics;
import com.yapp.d14.common.properties.S3Properties;
import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
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
    private final VideoCompositeMetrics videoCompositeMetrics;

    @Override
    public void compose(UUID userId, Long sessionId, List<AudioTrack> tracks) {
        Path workDir = null;
        Timer.Sample sample = videoCompositeMetrics.start();
        try {
            workDir = Files.createTempDirectory("composite-%s-".formatted(sessionId));

            Path rawVideo = workDir.resolve("raw.mp4");
            download(S3KeyGenerator.interviewRecordingKey(userId, sessionId), rawVideo);

            // 답변 음성은 제출 시 비동기로 저장되므로, 일부 트랙이 아직 없거나(경합) 업로드 실패로 없을 수 있다.
            // 없는 트랙만 개별 제외하고 나머지로 합성한다 — 트랙 하나 때문에 세션 전체 영상을 날리지 않기 위함.
            List<AudioTrack> availableTracks = new ArrayList<>();
            List<Path> audioFiles = new ArrayList<>();
            for (int i = 0; i < tracks.size(); i++) {
                AudioTrack track = tracks.get(i);
                Path audio = workDir.resolve("audio%d".formatted(i));
                try {
                    download(track.s3Key(), audio);
                    availableTracks.add(track);
                    audioFiles.add(audio);
                } catch (NoSuchKeyException e) {
                    log.warn("[COMPOSITE] 오디오 객체 없음, 해당 트랙 제외: sessionId={}, key={}", sessionId, track.s3Key());
                }
            }
            if (availableTracks.isEmpty()) {
                // 질문 TTS까지 하나도 못 받은 경우 — 무음 영상을 만드느니 실패로 두어 composited=false(videoUrl null)를 유지한다.
                throw new VideoCompositeFailedException("no_audio_track",
                        "[COMPOSITE] 합성할 오디오 트랙을 하나도 받지 못함: sessionId=%d".formatted(sessionId));
            }

            Path output = workDir.resolve("final.mp4");
            runFfmpeg(rawVideo, audioFiles, availableTracks, output, workDir.resolve("ffmpeg.log"));

            upload(S3KeyGenerator.interviewCompositeKey(userId, sessionId), output);
            videoCompositeMetrics.success(sample);
        } catch (IOException e) {
            videoCompositeMetrics.failure(sample, "io");
            throw new UncheckedIOException("[COMPOSITE] 임시파일 처리 실패: sessionId=%d".formatted(sessionId), e);
        } catch (InterruptedException e) {
            videoCompositeMetrics.failure(sample, "interrupted");
            Thread.currentThread().interrupt();
            throw new IllegalStateException("[COMPOSITE] 합성이 중단됨: sessionId=%d".formatted(sessionId), e);
        } catch (VideoCompositeFailedException e) {
            videoCompositeMetrics.failure(sample, e.reason());
            throw e;
        } catch (RuntimeException e) {
            videoCompositeMetrics.failure(sample, "other");
            throw e;
        } finally {
            cleanup(workDir);
        }
    }

    private void runFfmpeg(Path video, List<Path> audios, List<AudioTrack> tracks, Path output, Path ffmpegLog)
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

        command.add("-filter_complex");
        command.add(buildAudioFilter(tracks));
        command.add("-map");
        command.add("0:v");            // 녹화본은 영상 트랙만 사용한다(오디오는 서버가 만든 트랙으로 대체).
        command.add("-map");
        command.add("[aout]");
        command.add("-c:v");
        command.add("copy");
        command.add("-c:a");
        command.add("aac");
        // -shortest는 쓰지 않는다: 합성 오디오가 영상보다 먼저 끝나면(끝부분 무음 구간) 영상이 잘려버리기 때문.
        command.add(output.toString());

        // 파이프 대신 로그 파일로 리다이렉트해 stdout/stderr 버퍼 고갈로 인한 교착을 피한다.
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ffmpegLog.toFile())
                .start();

        boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new VideoCompositeFailedException("timeout",
                    "[COMPOSITE] ffmpeg 타임아웃(%ds 초과)".formatted(PROCESS_TIMEOUT_SECONDS));
        }
        if (process.exitValue() != 0) {
            throw new VideoCompositeFailedException("exit_code",
                    "[COMPOSITE] ffmpeg 종료코드 %d, log:%n%s".formatted(process.exitValue(), tail(ffmpegLog)));
        }
    }

    // 각 오디오 입력(1..N)을 시작초만큼 지연(adelay, ms)한 뒤 하나로 믹싱한다.
    // normalize=0: 입력 수에 따라 볼륨이 줄지 않도록(믹싱으로 개별 음성이 작아지는 것 방지).
    // ffmpeg 없이 필터 문자열을 검증할 수 있도록 package-private로 노출한다(FfmpegInterviewVideoCompositorAdapterTest).
    String buildAudioFilter(List<AudioTrack> tracks) {
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < tracks.size(); i++) {
            long delayMs = Math.max(0L, Math.round(tracks.get(i).startSec() * 1000.0));
            String label = tracks.size() == 1 ? "aout" : "a" + i;
            // 입력 인덱스는 1부터(0은 영상). 트랙이 1개면 지연 결과를 곧바로 [aout]으로 쓴다(amix 불필요).
            filter.append("[%d:a]adelay=%d:all=1[%s];".formatted(i + 1, delayMs, label));
        }
        if (tracks.size() == 1) {
            filter.setLength(filter.length() - 1); // 마지막 세미콜론 제거
            return filter.toString();
        }
        for (int i = 0; i < tracks.size(); i++) {
            filter.append("[a%d]".formatted(i));
        }
        filter.append("amix=inputs=%d:normalize=0[aout]".formatted(tracks.size()));
        return filter.toString();
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
