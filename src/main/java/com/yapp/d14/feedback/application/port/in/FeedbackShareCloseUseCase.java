package com.yapp.d14.feedback.application.port.in;

import com.yapp.d14.feedback.application.command.FeedbackShareCloseCommand;

public interface FeedbackShareCloseUseCase {

    /** 공유 링크를 비공개(되돌릴 수 없는 종료)로 전환한다. */
    void close(FeedbackShareCloseCommand command);
}
