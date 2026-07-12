package com.yapp.d14.jd.application.port.in;

import java.util.Optional;

public interface JdContentQueryUseCase {

    Optional<String> getContent(String jdUrl);
}
