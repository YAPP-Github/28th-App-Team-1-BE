package com.yapp.d14.interview.application.port.out;

import java.util.List;
import java.util.UUID;

public interface InterviewVideoCompositor {

    /**
     * 세션 녹화본(recording/raw.mp4)의 영상 트랙만 사용하고, 오디오는 서버가 보유한 트랙들(면접자 답변 음성 + 질문 TTS)을
     * 각 시작 시각으로 지연시켜 믹싱해 composite/final.mp4로 업로드한다. 녹화본 자체의 오디오 포함 여부에 의존하지 않는다.
     * 원본 비디오 스트림은 재인코딩 없이 그대로 복사한다(-c:v copy).
     * 실패 시 예외를 던지며, 부분 산출물(final.mp4)은 남기지 않는다.
     */
    void compose(UUID userId, Long sessionId, List<AudioTrack> tracks);

    /** s3Key: 오디오(답변 또는 질문 TTS)의 S3 key, startSec: 녹화 타임라인 기준 재생 시작 시각(초) */
    record AudioTrack(String s3Key, float startSec) {
    }
}
