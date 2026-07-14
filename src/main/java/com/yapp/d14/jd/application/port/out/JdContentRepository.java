package com.yapp.d14.jd.application.port.out;

import java.util.Optional;

public interface JdContentRepository {

    void save(String jdUrl, String content);

    boolean exists(String jdUrl);

    Optional<String> get(String jdUrl);
}
