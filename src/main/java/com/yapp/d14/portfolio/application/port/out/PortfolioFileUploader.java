package com.yapp.d14.portfolio.application.port.out;

public interface PortfolioFileUploader {

    void upload(String key, byte[] content, String contentType);

    void delete(String key);
}
