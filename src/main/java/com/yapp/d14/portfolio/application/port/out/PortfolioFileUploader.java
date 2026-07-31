package com.yapp.d14.portfolio.application.port.out;

public interface PortfolioFileUploader {

    void upload(String key, byte[] content, String contentType);

    void delete(String key);

    /** 저장된 파일 열람용 presigned GET URL을 발급한다. 유효시간은 어댑터가 정한다. */
    String presignDownload(String key);
}
