package com.yapp.d14.consent.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ConsentItem {

    AGE_OVER_14("만 14세 이상", true, 1),
    TERMS_OF_SERVICE("서비스 이용약관", true, 1),
    PERSONAL_INFO_COLLECTION("개인정보 수집·이용", true, 1),
    INTERVIEW_RECORDING("면접 영상·음성 촬영·저장", true, 1),
    OVERSEAS_TRANSFER("개인정보 국외 이전", true, 1);

    private final String label;
    private final boolean required;
    private final int currentVersion;

    public static List<ConsentItem> requiredItems() {
        return Arrays.stream(values())
                .filter(ConsentItem::isRequired)
                .toList();
    }
}
