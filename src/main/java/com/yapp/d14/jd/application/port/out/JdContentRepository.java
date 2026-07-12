package com.yapp.d14.jd.application.port.out;

public interface JdContentRepository {

    void save(String jdUrl, String content);

    boolean exists(String jdUrl);
}
