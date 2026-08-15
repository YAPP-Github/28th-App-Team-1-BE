package com.yapp.d14.interview.application.port.out;

public interface SpeechToTextTranscriber {

    TranscriptionResult transcribe(Long sessionId, byte[] audioContent);
}
