package com.yapp.d14.jd.application.service;

import com.yapp.d14.jd.application.port.in.JdValidationCheckUseCase;
import com.yapp.d14.jd.application.port.out.JdContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class JdValidationCheckService implements JdValidationCheckUseCase {

    private final JdContentRepository jdContentRepository;

    @Override
    public boolean isValidated(String jdUrl) {
        return jdContentRepository.exists(jdUrl);
    }
}
