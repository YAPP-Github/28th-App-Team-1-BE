package com.yapp.d14.portfolio.adapter.in.web.request;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

public record PortfolioRegisterHttpRequest(
        @NotBlank String fileName,
        @Positive long fileSize,
        @Positive int pageCount,
        @NotBlank String contentType
) {

    public PortfolioRegisterCommand toCommand(UUID userId, MultipartFile file) {
        try {
            return new PortfolioRegisterCommand(userId, file.getBytes(), fileName, fileSize, pageCount, contentType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
