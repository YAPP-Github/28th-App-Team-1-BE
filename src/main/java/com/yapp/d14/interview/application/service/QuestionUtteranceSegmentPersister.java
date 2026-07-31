package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import com.yapp.d14.interview.application.port.out.TranscriptionResult;
import com.yapp.d14.interview.application.port.out.UtteranceSegmentRepository;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.ScriptRole;
import com.yapp.d14.interview.domain.ScriptSegmentMapper;
import com.yapp.d14.interview.domain.UtteranceSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 리포트 생성 시점에 각 질문 TTS 음성을 STT로 재변환해 문장 단위 발화 시각을 저장한다(role=INTERVIEWER, #78).
// TTS는 문장별 타임스탬프를 주지 않으므로, 합성 영상에 실제로 얹히는 질문 음성을 다시 인식해 시각을 얻고
// questionStartSec만큼 더해 영상 타임라인으로 보정한다. 부가 기능이라 개별 질문 실패는 건너뛰고 진행한다.
@Slf4j
@Service
@RequiredArgsConstructor
class QuestionUtteranceSegmentPersister {

    private final QuestionRepository questionRepository;
    private final SpeechToTextTranscriber speechToTextTranscriber;
    private final InterviewVoiceStorage interviewVoiceStorage;
    private final UtteranceSegmentRepository utteranceSegmentRepository;

    void persist(UUID userId, Long sessionId) {
        // 재생성(리포트 재생성) 대비로 기존 INTERVIEWER 세그먼트만 지운 뒤 다시 채운다 — 면접 중 저장된 INTERVIEWEE는 보존.
        utteranceSegmentRepository.deleteBySessionIdAndRole(sessionId, ScriptRole.INTERVIEWER);
        List<Question> questions = questionRepository.findAllBySessionId(sessionId);
        // 질문마다 S3 GET + Whisper STT 호출(블로킹 I/O)이 있어 순차 처리하면 질문 수만큼 지연이 누적된다.
        // persistOne이 이미 실패를 개별적으로 흡수하므로, 가상 스레드로 병렬 처리해도 실패 격리는 그대로 유지된다.
        // try-with-resources가 close() 시점에 제출된 작업이 모두 끝날 때까지 대기한다(JEP 444 표준 패턴).
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            questions.forEach(question -> executor.execute(() -> persistOne(userId, sessionId, question)));
        }
    }

    private void persistOne(UUID userId, Long sessionId, Question question) {
        // 재생되지 않은(시작 시각·턴 없는) 질문은 영상에 안 얹히므로 건너뛴다.
        // 음성 키는 aiVoiceS3Key(요약 질문만 저장됨)에 의존하지 않고 turnLevel로 결정적 재계산한다 — 답변 음성과 동일 방식.
        if (question.getQuestionStartSec() == null || question.getTurnLevel() == null) {
            return;
        }
        try {
            String key = S3KeyGenerator.interviewVoiceKey(userId, sessionId, question.getTurnLevel());
            String base64 = interviewVoiceStorage.readBase64(key);
            if (base64 == null) {
                return;
            }
            byte[] audio = Base64.getDecoder().decode(base64);
            TranscriptionResult transcription = speechToTextTranscriber.transcribe(audio);
            List<UtteranceSegment> segments = ScriptSegmentMapper.map(
                    ScriptRole.INTERVIEWER, question.getContent(), transcription.segments(), question.getQuestionStartSec());
            utteranceSegmentRepository.saveAll(sessionId, question.getId(), segments);
        } catch (Exception e) {
            log.warn("[QUESTION SEGMENT] 질문 문장 발화 시각 저장 실패, 이 질문은 건너뜁니다: sessionId={}, questionId={}",
                    sessionId, question.getId(), e);
        }
    }
}
