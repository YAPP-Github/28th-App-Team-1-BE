package com.yapp.d14.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class S3KeyGenerator {

    public static String generate(S3Directory directory, UUID ownerId, UUID id, String extension) {
        return "%s/%s/%s.%s".formatted(directory.getPath(), ownerId, id, extension);
    }
}
