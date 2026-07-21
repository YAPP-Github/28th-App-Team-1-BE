package com.yapp.d14.feedback.application.port.in;

import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackReportView;

public interface GuestFeedbackReportQueryUseCase {

    /** 리포트 화면의 지인 피드백 섹션용. 제출한 지인이 없으면 participantCount=0, guests=빈 리스트. */
    GuestFeedbackReportView getForReport(Long sessionId);
}
