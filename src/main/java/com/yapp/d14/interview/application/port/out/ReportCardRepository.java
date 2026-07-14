package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ReportCard;

import java.util.List;

public interface ReportCardRepository {

    ReportCard save(ReportCard reportCard);

    void saveAll(List<ReportCard> reportCards);

    List<ReportCard> findAllBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
