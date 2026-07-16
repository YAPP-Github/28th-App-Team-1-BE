package com.yapp.d14.interview.application.command;

import com.yapp.d14.interview.domain.InterviewEndType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

public record InterviewAnswerSubmitCommand(
        Long sessionId,
        Long questionId,
        byte[] audioContent,
        Float questionAudioStartSec,
        Float questionAudioEndSec,
        Float answerStartSec,
        Float answerEndSec,
        Float answerDuration,
        InterviewEndType endType,
        Boolean isWrapUp
) {

    public static InterviewAnswerSubmitCommand of(
            Long sessionId,
            Long questionId,
            MultipartFile audio,
            Float questionAudioStartSec,
            Float questionAudioEndSec,
            Float answerStartSec,
            Float answerEndSec,
            Float answerDuration,
            String rawEndType,
            Boolean isWrapUp
    ) {
        InterviewEndType endType = parseEndType(rawEndType);
        byte[] audioContent = extractAudioContent(audio);
        validateAudioPresence(endType, audioContent);

        return new InterviewAnswerSubmitCommand(
                sessionId, questionId, audioContent,
                questionAudioStartSec, questionAudioEndSec, answerStartSec, answerEndSec, answerDuration,
                endType, isWrapUp
        );
    }

    private static InterviewEndType parseEndType(String rawEndType) {
        if (rawEndType == null || rawEndType.isBlank()) {
            return null;
        }
        try {
            return InterviewEndType.valueOf(rawEndType);
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_END_TYPE);
        }
    }

    private static byte[] extractAudioContent(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            return null;
        }
        try {
            return audio.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void validateAudioPresence(InterviewEndType endType, byte[] audioContent) {
        boolean hasAudio = audioContent != null;
        boolean invalid = switch (endType) {
            case null -> !hasAudio;
            case SKIP -> hasAudio;
            case MANUAL_END, HARD_CAP, EARLY_EXIT -> false;
            default -> false;
        };
        if (invalid) {
            throw new InterviewException(InterviewErrorCode.INVALID_AUDIO_PRESENCE);
        }
    }
}
