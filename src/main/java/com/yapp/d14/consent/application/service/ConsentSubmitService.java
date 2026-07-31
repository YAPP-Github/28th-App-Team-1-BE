package com.yapp.d14.consent.application.service;

import com.yapp.d14.consent.application.command.ConsentItemSubmission;
import com.yapp.d14.consent.application.command.ConsentSubmitCommand;
import com.yapp.d14.consent.application.port.in.ConsentSubmitUseCase;
import com.yapp.d14.consent.application.port.out.ConsentRecordRepository;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.domain.ConsentRecord;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;
import com.yapp.d14.ticket.application.port.in.TicketInitializeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class ConsentSubmitService implements ConsentSubmitUseCase {

    private final ConsentRecordRepository consentRecordRepository;
    private final TicketInitializeUseCase ticketInitializeUseCase;

    @Override
    @Transactional
    public void submit(ConsentSubmitCommand command) {
        List<ConsentRecord> existingRecords = consentRecordRepository.findAllByUserId(command.userId());
        boolean firstSubmission = existingRecords.isEmpty();

        validateVersions(command.items());
        validateRequiredItems(command.items(), firstSubmission);

        for (ConsentItemSubmission submission : command.items()) {
            consentRecordRepository.save(
                    ConsentRecord.create(command.userId(), submission.item(), submission.agreed())
            );
        }

        ticketInitializeUseCase.initialize(command.userId());
    }

    private void validateVersions(List<ConsentItemSubmission> submissions) {
        boolean anyVersionMismatch = submissions.stream()
                .anyMatch(submission -> submission.version() != submission.item().getCurrentVersion());
        if (anyVersionMismatch) {
            throw new ConsentException(ConsentErrorCode.CONSENT_VERSION_MISMATCH);
        }
    }

    private void validateRequiredItems(List<ConsentItemSubmission> submissions, boolean firstSubmission) {
        Map<ConsentItem, Boolean> agreedByItem = submissions.stream()
                .collect(Collectors.toMap(ConsentItemSubmission::item, ConsentItemSubmission::agreed));

        boolean anyRequiredDeclined = agreedByItem.entrySet().stream()
                .anyMatch(entry -> entry.getKey().isRequired() && !entry.getValue());
        if (anyRequiredDeclined) {
            throw new ConsentException(ConsentErrorCode.REQUIRED_CONSENT_MISSING);
        }

        if (firstSubmission) {
            boolean allRequiredPresent = ConsentItem.requiredItems().stream()
                    .allMatch(item -> Boolean.TRUE.equals(agreedByItem.get(item)));
            if (!allRequiredPresent) {
                throw new ConsentException(ConsentErrorCode.REQUIRED_CONSENT_MISSING);
            }
        }
    }
}
