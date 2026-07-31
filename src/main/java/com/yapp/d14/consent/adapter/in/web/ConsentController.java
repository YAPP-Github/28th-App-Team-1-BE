package com.yapp.d14.consent.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.consent.adapter.in.web.request.ConsentSubmitHttpRequest;
import com.yapp.d14.consent.adapter.in.web.response.ConsentDocumentHttpResponse;
import com.yapp.d14.consent.adapter.in.web.response.ConsentItemHttpResponse;
import com.yapp.d14.consent.adapter.in.web.response.ConsentPendingItemsHttpResponse;
import com.yapp.d14.consent.application.port.in.ConsentDocumentQueryUseCase;
import com.yapp.d14.consent.application.port.in.ConsentSubmitUseCase;
import com.yapp.d14.consent.application.port.in.PendingConsentItemsQueryUseCase;
import com.yapp.d14.consent.application.port.in.RequiredConsentStatusQueryUseCase;
import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.exception.ConsentErrorCode;
import com.yapp.d14.consent.exception.ConsentException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
@Validated
class ConsentController implements ConsentControllerDocs {

    private final ConsentSubmitUseCase consentSubmitUseCase;
    private final PendingConsentItemsQueryUseCase pendingConsentItemsQueryUseCase;
    private final RequiredConsentStatusQueryUseCase requiredConsentStatusQueryUseCase;
    private final ConsentDocumentQueryUseCase consentDocumentQueryUseCase;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(
            @CurrentUser UUID userId,
            @Valid @RequestBody ConsentSubmitHttpRequest request
    ) {
        consentSubmitUseCase.submit(request.toCommand(userId));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<ConsentPendingItemsHttpResponse>> getPendingItems(@CurrentUser UUID userId) {
        List<ConsentItemHttpResponse> items = pendingConsentItemsQueryUseCase.getPendingItems(userId).stream()
                .map(item -> ConsentItemHttpResponse.from(
                        item, consentDocumentQueryUseCase.hasDocument(item, item.getCurrentVersion())
                ))
                .toList();

        return ResponseEntity.ok(
                ApiResponse.ok(ConsentPendingItemsHttpResponse.of(requiredConsentStatusQueryUseCase.getStatus(userId), items))
        );
    }

    @Override
    @GetMapping("/{item}/versions/{version}")
    public ResponseEntity<ApiResponse<ConsentDocumentHttpResponse>> getDocument(
            @PathVariable String item,
            @PathVariable int version
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(ConsentDocumentHttpResponse.from(consentDocumentQueryUseCase.getDocument(parseItem(item), version)))
        );
    }

    private static ConsentItem parseItem(String rawItem) {
        try {
            return ConsentItem.valueOf(rawItem);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ConsentException(ConsentErrorCode.INVALID_CONSENT_ITEM);
        }
    }
}
