package com.yapp.d14.jd.exception;

// BusinessException을 상속하지 않는 내부 신호용 예외. JdContentFetcher 구현체가 던지면
// JdValidateService가 잡아 JdCrawlResult.failure(CRAWLING_FAILED)로 변환해야 하며,
// GlobalExceptionHandler까지 전파되어서는 안 된다.
public class JdCrawlingFailedException extends RuntimeException {

    public JdCrawlingFailedException(String message) {
        super(message);
    }

    public JdCrawlingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
