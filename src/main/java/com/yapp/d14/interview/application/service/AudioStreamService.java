package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.AudioStreamUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class AudioStreamService implements AudioStreamUseCase {

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final TextToSpeechSynthesizer textToSpeechSynthesizer;

    @Override
    public Flux<byte[]> stream(UUID userId, Long sessionId, Long questionId) {
        InterviewSession session = interviewSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        Question question = questionRepository.findById(questionId)
                .filter(q -> q.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new InterviewException(InterviewErrorCode.QUESTION_NOT_FOUND));

        // TODO: S3 tee 저장(클라이언트로 나가는 동일 스트림을 doOnNext로 버퍼링 후 doOnComplete에 업로드)은 다음 작업에서 이어붙인다.
        return textToSpeechSynthesizer.synthesizeStream(question.getContent());
    }
}
