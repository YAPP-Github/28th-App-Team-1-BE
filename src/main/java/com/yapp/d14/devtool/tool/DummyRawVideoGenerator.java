package com.yapp.d14.devtool.tool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 실제로 업로드·합성될 "녹화본" 대역의 무음 테스트 영상을 ffmpeg로 만든다.
 * 프로덕션에서는 클라이언트가 녹화한 영상을 presigned URL로 직접 올리므로 서버 쪽에 원본 영상 생성 경로가 없다 —
 * FfmpegInterviewVideoCompositorAdapterSmokeTest가 쓰는 것과 동일한 lavfi testsrc 기법을 재사용한다.
 */
final class DummyRawVideoGenerator {

    private DummyRawVideoGenerator() {
    }

    static Path generate(float durationSec) {
        int seconds = Math.max(1, (int) Math.ceil(durationSec));
        try {
            Path outputFile = Files.createTempFile("dummy-raw-", ".mp4");
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg", "-y", "-f", "lavfi", "-i", "testsrc=duration=" + seconds + ":size=320x240:rate=30",
                    "-pix_fmt", "yuv420p", outputFile.toString()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                throw new IllegalStateException("ffmpeg 원본 영상 생성 실패: " + output);
            }
            return outputFile;
        } catch (IOException e) {
            throw new UncheckedIOException("원본 영상 생성 중 IO 오류가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("원본 영상 생성이 중단되었습니다.", e);
        }
    }
}
