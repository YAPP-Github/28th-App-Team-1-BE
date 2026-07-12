package com.yapp.d14.interview.adapter.out.integration.tts;

import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import org.springframework.stereotype.Component;

// TODO: 실제 TTS 연동 시 이 클래스를 교체. AWS Polly로 mp3를 합성해 S3에 업로드하고
// (PortfolioFileUploader/S3PortfolioFileUploaderAdapter와 동일한 S3Client 업로드 패턴 재사용)
// 업로드된 S3 key를 반환 — Question.aiVoiceS3Key에 그대로 저장된다.
@Component
class StubTextToSpeechSynthesizerAdapter implements TextToSpeechSynthesizer {

    @Override
    public String synthesize(String text) {
        return null;
    }
}
