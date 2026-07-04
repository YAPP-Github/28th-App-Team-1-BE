package com.yapp.d14.jd.exception;

public class JdCrawlingFailedException extends RuntimeException {

    public JdCrawlingFailedException(String message) {
        super(message);
    }

    public JdCrawlingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
