package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ScriptRole;
import com.yapp.d14.interview.domain.UtteranceSegment;

import java.util.List;
import java.util.Map;

// 문장 단위 발화 시각(질문/답변 세그먼트)의 저장/조회. 질문·답변을 한 테이블에 role로 구분해 담는다(#78).
public interface UtteranceSegmentRepository {

    // 한 턴(questionId)의 세그먼트를 role 그대로 저장한다. 비어 있으면 아무것도 하지 않는다.
    void saveAll(Long sessionId, Long questionId, List<UtteranceSegment> segments);

    // 세션의 모든 세그먼트를 questionId별로 묶어 반환한다(각 리스트는 startSec 오름차순).
    Map<Long, List<UtteranceSegment>> findBySessionIdGroupedByQuestionId(Long sessionId);

    // 재생성(리포트 재생성 등) 시 중복을 막기 위한 삭제. role 단위로 지워, 면접 중 저장된 다른 role(INTERVIEWEE)은 건드리지 않는다.
    void deleteBySessionIdAndRole(Long sessionId, ScriptRole role);
}
