package com.yapp.d14.portfolio.adapter.in.web.request;

import com.yapp.d14.portfolio.application.command.PortfolioRegisterCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

public record PortfolioRegisterHttpRequest(
        @Schema(description = "업로드 파일명", example = "portfolio.pdf")
        @NotBlank String fileName,

        @Schema(description = "파일 크기(byte), 20MB 이하", example = "1048576")
        @Positive long fileSize,

        @Schema(description = "PDF 페이지 수, 30페이지 이하", example = "12")
        @Positive int pageCount,

        @Schema(description = "파일 MIME 타입", example = "application/pdf")
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
