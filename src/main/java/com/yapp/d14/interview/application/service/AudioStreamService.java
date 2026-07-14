package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.AudioStreamUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class AudioStreamService implements AudioStreamUseCase {

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final TextToSpeechSynthesizer textToSpeechSynthesizer;
    private final InterviewVoiceStorage interviewVoiceStorage;

    @Override
    public Flux<byte[]> stream(UUID userId, Long sessionId, Long questionId) {
        InterviewSession session = interviewSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        Question question = questionRepository.findById(questionId)
                .filter(q -> q.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.QUESTION_NOT_FOUND));

        // tee: 클라이언트로 나가는 동일 스트림에서 청크를 버퍼링해뒀다가 완료 시점에 S3로 비동기 업로드한다.
        // Flux를 재구독해 새로 생성하면 OpenAI TTS 호출이 두 번 나가 비용이 두 배가 되므로 반드시 같은 구독에서 처리한다.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        return textToSpeechSynthesizer.synthesizeStream(question.getContent())
                .doOnNext(buffer::writeBytes)
                .doOnComplete(() -> archiveAsync(userId, sessionId, question.getTurnLevel(), buffer.toByteArray()))
                .doOnError(e -> log.warn(
                        "[AUDIO ARCHIVE] TTS 스트림 생성 실패, S3 업로드 생략: sessionId={}, questionId={}", sessionId, questionId, e));
    }

    private void archiveAsync(UUID userId, Long sessionId, int turnLevel, byte[] audioContent) {
        try {
            interviewVoiceStorage.uploadAsync(userId, sessionId, turnLevel, audioContent);
        } catch (Exception e) {
            log.warn("[AUDIO ARCHIVE] 업로드 요청 실패, 재생 흐름에는 영향 없음: sessionId={}, turnLevel={}", sessionId, turnLevel, e);
        }
    }
}
