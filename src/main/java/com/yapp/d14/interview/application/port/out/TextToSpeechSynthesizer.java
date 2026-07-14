package com.yapp.d14.interview.application.port.out;

public interface TextToSpeechSynthesizer {

    byte[] synthesize(String text);
}
