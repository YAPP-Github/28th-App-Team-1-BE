package com.yapp.d14.devtool.tool;

import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 답변 제출 파이프라인(STT→LLM 채점)을 실제로 태우기 위해, 실제 TTS로 답변 텍스트를 발화시킨 뒤
 * Whisper 어댑터가 요구하는 m4a(AAC) 컨테이너로 트랜스코딩한다.
 * OpenAiSpeechToTextTranscriberAdapter가 파일명을 answer.m4a로 고정하므로 원본 포맷(mp3)을 그대로 넘기면 인식에 실패한다.
 *
 * TextToSpeechSynthesizer(Port-out)를 devtool이 직접 호출하는 것은 이 클래스에 한정된 예외다 —
 * 프로덕션에는 "답변을 TTS로 합성"하는 유스케이스 자체가 없어(실제 면접자는 육성으로 답변) 대응되는 in-port가 존재하지 않는다.
 * Application Service는 Port-out을 이렇게 바깥에서 직접 꺼내 쓰지 않고 항상 자신의 생성자로 주입받아야 한다 — 이 패턴을 따라 하지 말 것.
 */
final class DummyAnswerAudioGenerator {

    private static final List<String> ANSWER_TEXTS = List.of(
            "네, 저는 이전 프로젝트에서 API 응답 지연 문제를 데이터베이스 인덱스 튜닝과 캐싱 도입으로 해결한 경험이 있습니다. "
                    + "문제를 지표로 정의하고 원인을 좁혀가는 과정을 중요하게 생각합니다.",
            "제가 맡았던 프로젝트에서는 트래픽이 급증하는 상황에서 장애를 방지하기 위해 재시도 로직과 요청 제한을 도입했습니다. "
                    + "이 과정에서 팀원들과 충분히 논의하며 진행했습니다.",
            "저는 새로운 기술을 도입할 때 팀에 미치는 영향을 먼저 고려합니다. 최근에는 비동기 처리 도입을 제안하고, "
                    + "성능 개선 효과를 수치로 공유해 팀의 동의를 얻었습니다."
    );

    record AnswerAudio(byte[] content, float durationSec) {
    }

    private DummyAnswerAudioGenerator() {
    }

    static AnswerAudio synthesizeAnswerAudio(TextToSpeechSynthesizer textToSpeechSynthesizer, int turnIndex) {
        String text = ANSWER_TEXTS.get(turnIndex % ANSWER_TEXTS.size());
        byte[] speechBytes = textToSpeechSynthesizer.synthesize(null, text);
        return transcodeToM4a(speechBytes);
    }

    private static AnswerAudio transcodeToM4a(byte[] sourceAudio) {
        try {
            Path sourceFile = Files.createTempFile("dummy-answer-", ".mp3");
            Path targetFile = Files.createTempFile("dummy-answer-", ".m4a");
            try {
                Files.write(sourceFile, sourceAudio);
                runFfmpeg(sourceFile, targetFile);
                byte[] m4aBytes = Files.readAllBytes(targetFile);
                return new AnswerAudio(m4aBytes, probeDurationSec(m4aBytes, ".m4a"));
            } finally {
                Files.deleteIfExists(sourceFile);
                Files.deleteIfExists(targetFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("답변 음성 트랜스코딩 중 IO 오류가 발생했습니다.", e);
        }
    }

    private static void runFfmpeg(Path sourceFile, Path targetFile) throws IOException {
        run(new ProcessBuilder("ffmpeg", "-y", "-i", sourceFile.toString(), "-c:a", "aac", targetFile.toString()));
    }

    /** 질문 TTS 오디오(원본 mp3 바이트 등) 길이 측정에도 재사용된다 — 타임라인을 실제 오디오 길이에 맞추기 위해 필요하다. */
    static float probeDurationSec(byte[] audioContent, String fileSuffix) {
        try {
            Path tempFile = Files.createTempFile("dummy-audio-", fileSuffix);
            try {
                Files.write(tempFile, audioContent);
                String output = run(new ProcessBuilder(
                        "ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", tempFile.toString()
                ));
                return Float.parseFloat(output.trim());
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("오디오 길이 측정 중 IO 오류가 발생했습니다.", e);
        }
    }

    private static String run(ProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                throw new IllegalStateException("명령 실행 실패(" + processBuilder.command() + "): " + output);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("명령 실행이 중단되었습니다: " + processBuilder.command(), e);
        }
        return output;
    }
}
