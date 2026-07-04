package com.yapp.d14.jd.application.port.out;

import java.util.UUID;

public interface JdContentCache {

    void save(UUID userId, String jdUrl, String content);
}
