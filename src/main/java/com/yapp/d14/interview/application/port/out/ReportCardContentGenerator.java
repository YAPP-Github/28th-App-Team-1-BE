package com.yapp.d14.interview.application.port.out;

import java.util.List;

public interface ReportCardContentGenerator {

    List<ReportCardDraft> generate(Long sessionId, ReportCardContentContext context);
}
