package com.yapp.d14.interview.adapter.in.web.request;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

// POST /answers 요청 메타데이터(설계 문서 7-2장). clientRequestId(idempotency)는 이번 이슈 범위에서 제외.
// endType·isWrapUp·questionAudioStartAt/EndAt은 계약대로 받아두되, 이번 turnLevel=0 경로에서는 사용하지 않는다(이슈2에서 활용).
public record InterviewAnswerSubmitHttpRequest(
        @Schema(description = "클라이언트가 알고 있는 턴 순번", example = "0")
        @NotNull Integer turnLevel,

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
        Boolean isWrapUp
) {

    public InterviewAnswerSubmitCommand toCommand(Long sessionId, MultipartFile audio) {
        try {
            return new InterviewAnswerSubmitCommand(
                    sessionId, questionId, turnLevel, audio.getBytes(), answerStartAt, answerEndAt, answerDuration
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
