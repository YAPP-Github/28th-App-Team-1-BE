package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.command.ConsentItemSubmission;
import com.yapp.d14.consent.application.command.ConsentSubmitCommand;
import com.yapp.d14.consent.application.port.out.ConsentRecordRepository;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.domain.ConsentRecord;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;
import com.yapp.d14.ticket.application.port.in.TicketInitializeUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsentSubmitServiceTest {

    @Mock
    private ConsentRecordRepository consentRecordRepository;

    @Mock
    private TicketInitializeUseCase ticketInitializeUseCase;

    @InjectMocks
    private ConsentSubmitService service;

    private final UUID userId = UUID.randomUUID();

    private static List<ConsentItemSubmission> allRequiredAgreed() {
        return ConsentItem.requiredItems().stream()
                .map(item -> new ConsentItemSubmission(item, item.getCurrentVersion(), true))
                .toList();
    }

    @Test
    void 최초_제출이고_필수_5종을_모두_동의하면_저장하고_이용권을_초기화한다() {
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(List.of());

        service.submit(new ConsentSubmitCommand(userId, allRequiredAgreed()));

        verify(consentRecordRepository, times(ConsentItem.requiredItems().size())).save(any());
        verify(ticketInitializeUseCase).initialize(userId);
    }

    @Test
    void 최초_제출인데_필수_항목이_하나라도_빠지면_예외를_던지고_저장하지_않는다() {
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(List.of());
        List<ConsentItemSubmission> missingOne = allRequiredAgreed().subList(0, ConsentItem.requiredItems().size() - 1);

        assertThatThrownBy(() -> service.submit(new ConsentSubmitCommand(userId, missingOne)))
                .isInstanceOf(ConsentException.class)
                .extracting(e -> ((ConsentException) e).getErrorCode())
                .isEqualTo(ConsentErrorCode.REQUIRED_CONSENT_MISSING);

        verify(consentRecordRepository, never()).save(any());
        verify(ticketInitializeUseCase, never()).initialize(any());
    }

    @Test
    void 필수_항목을_agreed_false로_보내면_최초든_재동의든_예외를_던진다() {
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(List.of());
        ConsentItemSubmission declined = new ConsentItemSubmission(
                ConsentItem.TERMS_OF_SERVICE, ConsentItem.TERMS_OF_SERVICE.getCurrentVersion(), false
        );

        assertThatThrownBy(() -> service.submit(new ConsentSubmitCommand(userId, List.of(declined))))
                .isInstanceOf(ConsentException.class)
                .extracting(e -> ((ConsentException) e).getErrorCode())
                .isEqualTo(ConsentErrorCode.REQUIRED_CONSENT_MISSING);

        verify(consentRecordRepository, never()).save(any());
    }

    @Test
    void 버전이_현행_버전과_다르면_예외를_던진다() {
        ConsentItemSubmission staleVersion = new ConsentItemSubmission(ConsentItem.TERMS_OF_SERVICE, 0, true);

        assertThatThrownBy(() -> service.submit(new ConsentSubmitCommand(userId, List.of(staleVersion))))
                .isInstanceOf(ConsentException.class)
                .extracting(e -> ((ConsentException) e).getErrorCode())
                .isEqualTo(ConsentErrorCode.CONSENT_VERSION_MISMATCH);

        verify(consentRecordRepository, never()).save(any());
        verify(ticketInitializeUseCase, never()).initialize(any());
    }

    @Test
    void 동일_항목이_중복_제출되면_예외를_던지고_저장하지_않는다() {
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(List.of());
        ConsentItemSubmission duplicate = new ConsentItemSubmission(
                ConsentItem.TERMS_OF_SERVICE, ConsentItem.TERMS_OF_SERVICE.getCurrentVersion(), true
        );

        assertThatThrownBy(() -> service.submit(new ConsentSubmitCommand(userId, List.of(duplicate, duplicate))))
                .isInstanceOf(ConsentException.class)
                .extracting(e -> ((ConsentException) e).getErrorCode())
                .isEqualTo(ConsentErrorCode.DUPLICATE_CONSENT_ITEM);

        verify(consentRecordRepository, never()).save(any());
        verify(ticketInitializeUseCase, never()).initialize(any());
    }

    @Test
    void 이미_동의_이력이_있으면_재동의로_보고_보낸_항목만_갱신하며_이용권_초기화는_멱등하게_호출된다() {
        ConsentRecord existing = ConsentRecord.of(
                userId, ConsentItem.TERMS_OF_SERVICE, 1, true, LocalDateTime.now()
        );
        given(consentRecordRepository.findAllByUserId(userId)).willReturn(List.of(existing));

        ConsentItemSubmission reconsent = new ConsentItemSubmission(
                ConsentItem.TERMS_OF_SERVICE, ConsentItem.TERMS_OF_SERVICE.getCurrentVersion(), true
        );
        service.submit(new ConsentSubmitCommand(userId, List.of(reconsent)));

        verify(consentRecordRepository, times(1)).save(any());
        verify(ticketInitializeUseCase).initialize(userId);
    }
}
