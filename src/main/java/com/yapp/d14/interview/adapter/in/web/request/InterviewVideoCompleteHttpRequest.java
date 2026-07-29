package com.yapp.d14.interview.adapter.in.web.request;

import com.yapp.d14.interview.application.command.InterviewVideoUploadCompleteCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

// 영상 업로드 완료 시 마무리 멘트(면접관 종료 TTS) 재생 구간을 함께 보고한다. 마무리 멘트가 없으면(조기 종료 등)
// 두 값 모두 생략(null)하며, 바디 전체를 비워도 된다(하위 호환).
public record InterviewVideoCompleteHttpRequest(
        @Schema(description = "마무리 멘트 재생 시작 시각 — 녹화(합성 영상) 타임라인 기준(초). 마무리 멘트가 없으면 null", example = "512.4")
        Float wrapUpStartSec,

        @Schema(description = "마무리 멘트 재생 종료 시각 — 녹화 타임라인 기준(초). 없으면 null", example = "515.8")
        Float wrapUpEndSec
) {

    // 바디 전체가 생략될 수 있어(@RequestBody required=false) 인스턴스 메서드가 아닌 정적 메서드로 둔다 — request가 null이면 두 값 모두 null인 커맨드를 만든다.
    public static InterviewVideoUploadCompleteCommand toCommand(
            UUID userId, Long sessionId, InterviewVideoCompleteHttpRequest request
    ) {
        Float wrapUpStartSec = request == null ? null : request.wrapUpStartSec();
        Float wrapUpEndSec = request == null ? null : request.wrapUpEndSec();
        return new InterviewVideoUploadCompleteCommand(userId, sessionId, wrapUpStartSec, wrapUpEndSec);
    }
}
