package com.yapp.d14.interview.application.command;

import java.util.UUID;

// wrapUpStartSec/wrapUpEndSec: 마무리 멘트(면접관 종료 TTS)의 녹화 타임라인 재생 구간(초). 마무리 멘트가 없으면(조기 종료 등) 둘 다 null.
public record InterviewVideoUploadCompleteCommand(
        UUID userId,
        Long sessionId,
        Float wrapUpStartSec,
        Float wrapUpEndSec
) {
}
