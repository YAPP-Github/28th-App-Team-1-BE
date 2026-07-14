package com.yapp.d14.jd.application.service;

import com.yapp.d14.jd.application.port.in.JdContentQueryUseCase;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class JdContentQueryService implements JdContentQueryUseCase {

    private final JdContentRepository jdContentRepository;

    @Override
    public Optional<String> getContent(String jdUrl) {
        return jdContentRepository.get(jdUrl);
    }
}
