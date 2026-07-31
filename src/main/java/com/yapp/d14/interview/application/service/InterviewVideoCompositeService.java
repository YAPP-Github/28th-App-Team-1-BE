package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.in.InterviewVideoCompositeUseCase;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor.AudioTrack;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewEndType;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.WrapUpMessage;
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
    private final InterviewSessionRepository interviewSessionRepository;

    @Override
    @Async("interviewCompositeTaskExecutor")
    public void composite(UUID userId, Long sessionId) {
        List<Question> questions = questionRepository.findAllBySessionId(sessionId);
        List<AudioTrack> tracks = new ArrayList<>();
        tracks.addAll(questionTracks(userId, sessionId, questions));
        tracks.addAll(answerTracks(userId, sessionId, questions));
        tracks.addAll(wrapUpTrack(sessionId));
        tracks.sort(Comparator.comparing(AudioTrack::startSec));

        if (tracks.isEmpty()) {
            log.warn("[COMPOSITE] 합성할 오디오 트랙이 없어 스킵: sessionId={}", sessionId);
            return;
        }

        try {
            interviewVideoCompositor.compose(userId, sessionId, tracks);
            int updated = interviewVideoRepository.markComposited(sessionId);
            if (updated == 0) {
                // final.mp4는 S3에 올라갔지만 표시할 영상 row가 없어 videoUrl이 계속 null이 된다(도달 불가 산출물).
                log.warn("[COMPOSITE] 합성본은 업로드됐으나 표시할 영상 row가 없음(marked=0): sessionId={}", sessionId);
            } else {
                log.info("[COMPOSITE] 합성 완료: sessionId={}, trackCount={}", sessionId, tracks.size());
            }
        } catch (Exception e) {
            // 합성 실패는 리포트/피드백 흐름을 막지 않는다. videoUrl은 composited=false라 계속 null로 노출된다.
            log.error("[COMPOSITE] 합성 실패, videoUrl은 null로 유지: sessionId={}", sessionId, e);
        }
    }

    // 질문 TTS: 재생 시각(녹화 타임라인)이 있는 질문만. 키는 답변과 동일하게 turnLevel로 결정적 재계산한다.
    // (aiVoiceS3Key는 요약 질문만 저장돼 있어 후속 질문 음성이 통째로 빠지므로 의존하지 않는다.)
    private List<AudioTrack> questionTracks(UUID userId, Long sessionId, List<Question> questions) {
        return questions.stream()
                .filter(question -> question.getQuestionStartSec() != null && question.getTurnLevel() != null)
                .map(question -> new AudioTrack(
                        S3KeyGenerator.interviewVoiceKey(userId, sessionId, question.getTurnLevel()),
                        question.getQuestionStartSec()))
                .toList();
    }

    // 면접관 마무리 멘트: 프론트가 보고한 재생 시각(wrapUpStartSec)이 있고 종료 유형에 마무리 문구가 있을 때만 얹는다.
    // 음성 키는 종료 유형별 공용 키(system/interview/wrapup-messages/{endType}.mp3). 답변·질문 음성과 달리 세션 무관하게 공유된다.
    // 객체가 없으면(EARLY_EXIT 등) 컴포지터가 NoSuchKeyException으로 해당 트랙만 건너뛴다.
    private List<AudioTrack> wrapUpTrack(Long sessionId) {
        Float startSec = interviewVideoRepository.findBySessionId(sessionId)
                .map(InterviewVideo::getWrapUpStartSec)
                .orElse(null);
        if (startSec == null) {
            return List.of();
        }
        InterviewEndType endType = interviewSessionRepository.findById(sessionId)
                .map(session -> session.getEndType())
                .orElse(null);
        if (WrapUpMessage.textFor(endType) == null) {
            return List.of();
        }
        return List.of(new AudioTrack(S3KeyGenerator.wrapUpMessageKey(endType.name()), startSec));
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
