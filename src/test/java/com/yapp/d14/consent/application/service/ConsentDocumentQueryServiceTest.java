package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.port.out.ConsentDocumentRepository;
import com.yapp.d14.consent.domain.ConsentDocument;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConsentDocumentQueryServiceTest {

    @Mock
    private ConsentDocumentRepository consentDocumentRepository;

    @InjectMocks
    private ConsentDocumentQueryService service;

    @Test
    void 문서가_있으면_그대로_반환한다() {
        ConsentDocument document = ConsentDocument.of(ConsentItem.TERMS_OF_SERVICE, 1, "이용약관", "본문");
        given(consentDocumentRepository.findByItemAndVersion(ConsentItem.TERMS_OF_SERVICE, 1))
                .willReturn(Optional.of(document));

        assertThat(service.getDocument(ConsentItem.TERMS_OF_SERVICE, 1)).isEqualTo(document);
    }

    @Test
    void 문서가_없으면_예외를_던진다() {
        given(consentDocumentRepository.findByItemAndVersion(ConsentItem.TERMS_OF_SERVICE, 99))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDocument(ConsentItem.TERMS_OF_SERVICE, 99))
                .isInstanceOf(ConsentException.class)
                .extracting(e -> ((ConsentException) e).getErrorCode())
                .isEqualTo(ConsentErrorCode.DOCUMENT_NOT_FOUND);
    }

    @Test
    void 문서_존재_여부를_그대로_반환한다() {
        given(consentDocumentRepository.existsByItemAndVersion(ConsentItem.OVERSEAS_TRANSFER, 1)).willReturn(false);

        assertThat(service.hasDocument(ConsentItem.OVERSEAS_TRANSFER, 1)).isFalse();
    }
}
