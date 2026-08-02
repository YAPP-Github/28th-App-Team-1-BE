package com.yapp.d14.interview.application.command;

import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;

import java.util.EnumSet;
import java.util.Set;

public record InterviewAbandonCommand(Long sessionId, AbandonCause cause) {

    // HOLD_EXPIRED는 서버가 스스로 정리할 때만 쓰는 내부 값 — 클라이언트 입력으로는 받지 않는다.
    private static final Set<AbandonCause> CLIENT_ALLOWED = EnumSet.of(AbandonCause.NETWORK_DISCONNECT, AbandonCause.USER_EXIT);

    public static InterviewAbandonCommand of(Long sessionId, String rawCause) {
        return new InterviewAbandonCommand(sessionId, parseCause(rawCause));
    }

    private static AbandonCause parseCause(String rawCause) {
        if (rawCause == null || rawCause.isBlank()) {
            throw new InterviewException(InterviewErrorCode.INVALID_ABANDON_CAUSE);
        }
        AbandonCause cause;
        try {
            cause = AbandonCause.valueOf(rawCause);
        } catch (IllegalArgumentException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_ABANDON_CAUSE);
        }
        // valueOf는 HOLD_EXPIRED도 성공시키므로, "정의된 값인지"와 별개로 화이트리스트로 한 번 더 걸러야 한다.
        if (!CLIENT_ALLOWED.contains(cause)) {
            throw new InterviewException(InterviewErrorCode.INVALID_ABANDON_CAUSE);
        }
        return cause;
    }
}
