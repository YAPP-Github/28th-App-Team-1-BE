package com.yapp.d14.interview.application.port.out;

public interface ReportHeadlineGenerator {

    String generate(Long sessionId, HeadlineContext context);
}
