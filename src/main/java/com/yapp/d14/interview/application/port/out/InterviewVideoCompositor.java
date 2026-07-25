package com.yapp.d14.interview.application.port.out;

import java.util.List;
import java.util.UUID;

public interface InterviewVideoCompositor {

    /**
     * 세션 녹화본(recording/raw.mp4)에 질문 TTS 음성을 각 시작 시각으로 지연시켜 면접자 오디오와 믹싱한 뒤
     * composite/final.mp4로 업로드한다. 원본 비디오 스트림은 재인코딩 없이 그대로 복사한다(-c:v copy).
     * 실패 시 예외를 던지며, 부분 산출물(final.mp4)은 남기지 않는다.
     */
    void compose(UUID userId, Long sessionId, List<QuestionAudioTrack> tracks);

    /** audioS3Key: 질문 TTS mp3의 S3 key, startSec: 녹화 타임라인 기준 재생 시작 시각(초) */
    record QuestionAudioTrack(String audioS3Key, float startSec) {
    }
}
