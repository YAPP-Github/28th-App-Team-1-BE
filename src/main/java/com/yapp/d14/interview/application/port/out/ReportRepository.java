package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.Report;

import java.util.Optional;

public interface ReportRepository {

    Report save(Report report);

    Optional<Report> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
