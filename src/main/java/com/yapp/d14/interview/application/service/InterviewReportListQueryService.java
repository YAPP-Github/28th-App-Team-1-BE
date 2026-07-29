package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportListQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportListItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// TODO(#81): 세션·레포트 조회 + 포트폴리오 삭제 판정 + 공유링크 존재 조회를 조합해 실제 목록을 반환한다. (현재는 컨트랙트용 스텁)
@Service
class InterviewReportListQueryService implements InterviewReportListQueryUseCase {

    @Override
    public List<InterviewReportListItem> getReportList(UUID userId) {
        return List.of();
    }
}
