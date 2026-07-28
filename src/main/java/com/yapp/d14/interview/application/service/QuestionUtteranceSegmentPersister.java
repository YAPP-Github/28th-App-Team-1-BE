package com.yapp.d14.interview.application.service;

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

// 리포트 생성 시점에 각 질문 TTS 음성을 STT로 재변환해 문장 단위 발화 시각을 저장한다(role=QUESTION, #78).
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

    void persist(Long sessionId) {
        // 재생성(리포트 재생성) 대비로 기존 QUESTION 세그먼트만 지운 뒤 다시 채운다 — 면접 중 저장된 ANSWER는 보존.
        utteranceSegmentRepository.deleteBySessionIdAndRole(sessionId, ScriptRole.QUESTION);
        for (Question question : questionRepository.findAllBySessionId(sessionId)) {
            persistOne(sessionId, question);
        }
    }

    private void persistOne(Long sessionId, Question question) {
        // 재생되지 않은(음성/시작 시각 없는) 질문은 영상에 안 얹히므로 건너뛴다.
        if (question.getAiVoiceS3Key() == null || question.getQuestionStartSec() == null) {
            return;
        }
        try {
            String base64 = interviewVoiceStorage.readBase64(question.getAiVoiceS3Key());
            if (base64 == null) {
                return;
            }
            byte[] audio = Base64.getDecoder().decode(base64);
            TranscriptionResult transcription = speechToTextTranscriber.transcribe(audio);
            List<UtteranceSegment> segments = ScriptSegmentMapper.map(
                    ScriptRole.QUESTION, question.getContent(), transcription.segments(), question.getQuestionStartSec());
            utteranceSegmentRepository.saveAll(sessionId, question.getId(), segments);
        } catch (Exception e) {
            log.warn("[QUESTION SEGMENT] 질문 문장 발화 시각 저장 실패, 이 질문은 건너뜁니다: sessionId={}, questionId={}",
                    sessionId, question.getId(), e);
        }
    }
}
