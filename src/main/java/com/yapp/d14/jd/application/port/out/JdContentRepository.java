package com.yapp.d14.jd.application.port.out;

import java.util.UUID;

public interface JdContentRepository {

    void save(UUID userId, String jdUrl, String content);
}
