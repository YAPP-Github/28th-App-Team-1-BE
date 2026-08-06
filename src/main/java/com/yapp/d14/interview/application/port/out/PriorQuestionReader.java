package com.yapp.d14.interview.application.port.out;

import java.util.List;
import java.util.UUID;

public interface PriorQuestionReader {

    // 같은 사용자의 이전 면접에서 실제 출제된 질문 원문. 최신순이며 구현체가 개수를 제한한다.
    List<String> readRecentQuestions(UUID userId, Long excludeSessionId);
}
