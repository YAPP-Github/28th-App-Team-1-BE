package com.yapp.d14.jd.application.port.in;

import lombok.Getter;

@Getter
public class JdCrawlResult {

    private final boolean valid;
    private final JdValidationFailureReason failureReason;
    private final String content;

    private JdCrawlResult(boolean valid, JdValidationFailureReason failureReason, String content) {
        this.valid = valid;
        this.failureReason = failureReason;
        this.content = content;
    }

    public static JdCrawlResult success(String content) {
        return new JdCrawlResult(true, null, content);
    }

    public static JdCrawlResult failure(JdValidationFailureReason failureReason) {
        return new JdCrawlResult(false, failureReason, null);
    }
}
