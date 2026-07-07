package com.yapp.d14.jd.exception;

// BusinessException을 상속하지 않는 내부 신호용 예외. JdContentExtractor 구현체가 던지면
// JdValidateService가 잡아 JdCrawlResult.failure(EXTRACTION_FAILED)로 변환해야 하며,
// GlobalExceptionHandler까지 전파되어서는 안 된다.
public class JdExtractionFailedException extends RuntimeException {

    public JdExtractionFailedException(String message) {
        super(message);
    }

    public JdExtractionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
