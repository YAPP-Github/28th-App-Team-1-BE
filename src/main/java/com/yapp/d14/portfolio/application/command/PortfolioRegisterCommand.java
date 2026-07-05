package com.yapp.d14.portfolio.application.command;

import java.util.UUID;

public record PortfolioRegisterCommand(
        UUID userId,
        byte[] fileContent,
        String fileName,
        long declaredFileSize,
        int declaredPageCount,
        String contentType
) {
}
