package com.yapp.d14.interview.application.port.in;

import java.util.UUID;

public interface InterviewSessionInProgressCheckUseCase {

    /** 해당 포트폴리오를 사용 중인 진행 중(IN_PROGRESS) 면접 세션이 있는지 확인한다. */
    boolean existsInProgress(UUID portfolioId);
}
