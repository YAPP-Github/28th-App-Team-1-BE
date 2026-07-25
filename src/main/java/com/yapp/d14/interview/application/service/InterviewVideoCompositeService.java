package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.in.InterviewVideoCompositeUseCase;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor.AudioTrack;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class InterviewVideoCompositeService implements InterviewVideoCompositeUseCase {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final InterviewVideoCompositor interviewVideoCompositor;
    private final InterviewVideoRepository interviewVideoRepository;

    @Override
    @Async("interviewCompositeTaskExecutor")
    public void composite(UUID userId, Long sessionId) {
        List<Question> questions = questionRepository.findAllBySessionId(sessionId);
        List<AudioTrack> tracks = new ArrayList<>();
        tracks.addAll(questionTracks(questions));
        tracks.addAll(answerTracks(userId, sessionId, questions));
        tracks.sort(Comparator.comparing(AudioTrack::startSec));

        if (tracks.isEmpty()) {
            log.warn("[COMPOSITE] 합성할 오디오 트랙이 없어 스킵: sessionId={}", sessionId);
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

    // 질문 TTS: 음성 키·재생 시각(녹화 타임라인)이 있는 질문만.
    private List<AudioTrack> questionTracks(List<Question> questions) {
        return questions.stream()
                .filter(question -> question.getAiVoiceS3Key() != null && question.getQuestionStartSec() != null)
                .map(question -> new AudioTrack(question.getAiVoiceS3Key(), question.getQuestionStartSec()))
                .toList();
    }

    // 답변 음성: SKIP·시작 시각 없는 답변은 제외. 키는 제출 시 저장한 것과 동일하게 turnLevel로 재계산한다.
    private List<AudioTrack> answerTracks(UUID userId, Long sessionId, List<Question> questions) {
        Map<Long, Integer> turnLevelByQuestionId = questions.stream()
                .filter(question -> question.getTurnLevel() != null)
                .collect(Collectors.toMap(Question::getId, Question::getTurnLevel));

        return answerRepository.findAllBySessionId(sessionId).stream()
                .filter(answer -> !Boolean.TRUE.equals(answer.getIsSkipped()) && answer.getAnswerStartSec() != null)
                .filter(answer -> turnLevelByQuestionId.containsKey(answer.getQuestionId()))
                .map(answer -> new AudioTrack(
                        S3KeyGenerator.interviewAnswerKey(userId, sessionId, turnLevelByQuestionId.get(answer.getQuestionId())),
                        answer.getAnswerStartSec()))
                .toList();
    }
}
