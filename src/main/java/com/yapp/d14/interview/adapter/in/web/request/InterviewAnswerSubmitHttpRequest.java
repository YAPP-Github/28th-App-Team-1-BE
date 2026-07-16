package com.yapp.d14.interview.adapter.in.web.request;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record InterviewAnswerSubmitHttpRequest(
        @Schema(description = "어떤 질문에 대한 제출인지", example = "1")
        @NotNull Long questionId,

        @Schema(description = "AI 질문 TTS 재생 시작 시간(영상 녹화 기준, 초)")
        Float questionAudioStartAt,

        @Schema(description = "AI 질문 TTS 재생 종료 시간(영상 녹화 기준, 초)")
        Float questionAudioEndAt,

        @Schema(description = "답변 시작 시간(초)")
        Float answerStartAt,

        @Schema(description = "답변 종료 시간(초)")
        Float answerEndAt,

        @Schema(description = "답변 길이(초)")
        Float answerDuration,

        @Schema(description = """
                종료·특수 처리 사유. null/SKIP/MANUAL_END/HARD_CAP/EARLY_EXIT 중 하나
                - null: 정상 진행 (일반 답변 제출)
                - SKIP: 답변을 건너뜀 (audio 없음)
                - MANUAL_END: 8:00 경과 후 사용자가 종료 버튼으로 수동 종료
                - HARD_CAP: 12:00 경과로 서버가 강제 종료
                - EARLY_EXIT: 0:00~8:00 사이 사용자가 의도적으로 이탈(차감 경고 확인 후)
                """)
        String endType,

        @Schema(description = "면접 시작 8:45 경과 여부(클라이언트 타이머 기준)")
        @NotNull Boolean isWrapUp
) {

    public InterviewAnswerSubmitCommand toCommand(Long sessionId, MultipartFile audio) {
        return InterviewAnswerSubmitCommand.of(
                sessionId, questionId, audio,
                questionAudioStartAt, questionAudioEndAt, answerStartAt, answerEndAt, answerDuration,
                endType, isWrapUp
        );
    }
}
