package com.yapp.d14.consent.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsentDocument {

    private final ConsentItem item;
    private final int version;
    private final String title;
    private final String content;

    public static ConsentDocument of(ConsentItem item, int version, String title, String content) {
        return ConsentDocument.builder()
                .item(item)
                .version(version)
                .title(title)
                .content(content)
                .build();
    }
}
