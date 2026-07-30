package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewReportListItem;

import java.util.List;
import java.util.UUID;

public interface InterviewReportListQueryUseCase {

    /**
     * 마이페이지 "내 면접 레포트" 목록.
     * 레포트 생성이 시도된(Report 행이 존재하는) 세션만 최신순으로 반환한다.
     */
    List<InterviewReportListItem> getReportList(UUID userId);
}
