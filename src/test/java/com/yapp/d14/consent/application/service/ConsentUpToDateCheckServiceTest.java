package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.in.RequiredConsentStatusQueryUseCase;
import com.yapp.d14.consent.domain.RequiredConsentStatus;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConsentUpToDateCheckServiceTest {

    @Mock
    private RequiredConsentStatusQueryUseCase requiredConsentStatusQueryUseCase;

    @InjectMocks
    private ConsentUpToDateCheckService consentUpToDateCheckService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 필수_동의가_최신이면_통과한다() {
        given(requiredConsentStatusQueryUseCase.getStatus(userId)).willReturn(RequiredConsentStatus.UP_TO_DATE);

        assertThatCode(() -> consentUpToDateCheckService.checkUpToDate(userId)).doesNotThrowAnyException();
    }

    @Test
    void 동의가_구버전이면_CONSENT_VERSION_STALE() {
        given(requiredConsentStatusQueryUseCase.getStatus(userId)).willReturn(RequiredConsentStatus.STALE);

        assertThatThrownBy(() -> consentUpToDateCheckService.checkUpToDate(userId))
                .isInstanceOf(ConsentException.class)
                .extracting(e -> ((ConsentException) e).getErrorCode())
                .isEqualTo(ConsentErrorCode.CONSENT_VERSION_STALE);
    }

    @Test
    void 필수_동의가_아예_없어도_같은_재동의_경로로_보낸다() {
        given(requiredConsentStatusQueryUseCase.getStatus(userId)).willReturn(RequiredConsentStatus.NOT_SUBMITTED);

        assertThatThrownBy(() -> consentUpToDateCheckService.checkUpToDate(userId))
                .isInstanceOf(ConsentException.class)
                .extracting(e -> ((ConsentException) e).getErrorCode())
                .isEqualTo(ConsentErrorCode.CONSENT_VERSION_STALE);
    }
}
