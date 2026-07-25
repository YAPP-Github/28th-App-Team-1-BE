package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewVideoCompositeUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor.QuestionAudioTrack;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewVideoCompositeService implements InterviewVideoCompositeUseCase {

    private final QuestionRepository questionRepository;
    private final InterviewVideoCompositor interviewVideoCompositor;
    private final InterviewVideoRepository interviewVideoRepository;

    @Override
    @Async("interviewCompositeTaskExecutor")
    public void composite(UUID userId, Long sessionId) {
        // 질문 TTS가 없거나 재생 시각(녹화 타임라인 기준)이 없는 질문은 합성할 수 없으므로 제외한다.
        List<QuestionAudioTrack> tracks = questionRepository.findAllBySessionId(sessionId).stream()
                .filter(question -> question.getAiVoiceS3Key() != null && question.getQuestionStartSec() != null)
                .sorted(Comparator.comparing(Question::getQuestionStartSec))
                .map(question -> new QuestionAudioTrack(question.getAiVoiceS3Key(), question.getQuestionStartSec()))
                .toList();

        if (tracks.isEmpty()) {
            log.warn("[COMPOSITE] 합성할 질문 오디오가 없어 스킵: sessionId={}", sessionId);
            return;
        }

        try {
            interviewVideoCompositor.compose(userId, sessionId, tracks);
            int updated = interviewVideoRepository.markComposited(sessionId);
            log.info("[COMPOSITE] 합성 완료: sessionId={}, trackCount={}, marked={}", sessionId, tracks.size(), updated);
        } catch (Exception e) {
            // 합성 실패는 리포트/피드백 흐름을 막지 않는다. videoUrl은 composited=false라 계속 null로 노출된다.
            log.error("[COMPOSITE] 합성 실패, videoUrl은 null로 유지: sessionId={}", sessionId, e);
        }
    }
}
