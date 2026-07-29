package com.yapp.d14.interview.application.port.in;

import java.util.UUID;

public interface InterviewVideoUploadCompleteUseCase {

    /**
     * 프론트가 S3 업로드를 끝낸 뒤 호출한다. 해당 세션 영상을 "업로드 완료"로 표시해 재생 URL 발급을 허용한다.
     * wrapUpStartSec/wrapUpEndSec은 마무리 멘트(면접관 종료 TTS)의 녹화 타임라인 재생 구간(초)으로,
     * 합성 영상에 마무리 멘트를 얹고 대본에 넣는 데 쓴다. 마무리 멘트가 없으면(조기 종료 등) 둘 다 null.
     */
    void complete(UUID userId, Long sessionId, Float wrapUpStartSec, Float wrapUpEndSec);
}
