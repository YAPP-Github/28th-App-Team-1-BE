package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.QuestionBoundaryResult;

import java.util.List;

public interface QuestionBoundaryQueryUseCase {

    /** 재생 시각이 기록된 질문만, 턴 순서대로 반환한다. */
    List<QuestionBoundaryResult> getQuestionBoundaries(Long sessionId);
}
