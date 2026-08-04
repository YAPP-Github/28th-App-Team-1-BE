package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.in.ConsentUpToDateCheckUseCase;
import com.yapp.d14.consent.application.port.in.RequiredConsentStatusQueryUseCase;
import com.yapp.d14.consent.domain.RequiredConsentStatus;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ConsentUpToDateCheckService implements ConsentUpToDateCheckUseCase {

    private final RequiredConsentStatusQueryUseCase requiredConsentStatusQueryUseCase;

    @Override
    public void checkUpToDate(UUID userId) {
        // NOT_SUBMITTED는 동의 제출 시점에 계정이 만들어지므로 정상 흐름에선 나오지 않지만,
        // 필수 항목이 새로 추가된 경우 STALE과 구분할 이유가 없어 같은 재동의 경로로 보낸다.
        if (requiredConsentStatusQueryUseCase.getStatus(userId) != RequiredConsentStatus.UP_TO_DATE) {
            throw new ConsentException(ConsentErrorCode.CONSENT_VERSION_STALE);
        }
    }
}
