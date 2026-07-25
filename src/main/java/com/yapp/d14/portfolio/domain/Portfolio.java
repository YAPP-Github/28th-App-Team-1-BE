package com.yapp.d14.portfolio.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Portfolio {

    private static final int MIN_EXTRACTED_TEXT_LENGTH = 300;
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(15);

    private final UUID id;
    private final UUID userId;
    private final String fileName;
    private final long fileSize;
    private Integer pageCount;
    private final String s3Key;
    private PortfolioStatus status;
    private String message;
    private final LocalDateTime createdAt;
    private LocalDateTime uploadedAt;
    private final boolean replacement;
    private boolean deleted;
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Portfolio(
            UUID id,
            UUID userId,
            String fileName,
            long fileSize,
            Integer pageCount,
            String s3Key,
            PortfolioStatus status,
            String message,
            LocalDateTime createdAt,
            LocalDateTime uploadedAt,
            boolean replacement,
            boolean deleted,
            LocalDateTime deletedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
        this.s3Key = s3Key;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
        this.uploadedAt = uploadedAt;
        this.replacement = replacement;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    public static Portfolio create(UUID id, UUID userId, String fileName, long fileSize, int pageCount, String s3Key, boolean replacement) {
        return Portfolio.builder()
                .id(id)
                .userId(userId)
                .fileName(fileName)
                .fileSize(fileSize)
                .pageCount(pageCount)
                .s3Key(s3Key)
                .status(PortfolioStatus.PROCESSING)
                .message("포트폴리오를 분석하고 있어요.")
                .createdAt(LocalDateTime.now())
                .replacement(replacement)
                .deleted(false)
                .build();
    }

    public static Portfolio of(
            UUID id,
            UUID userId,
            String fileName,
            long fileSize,
            Integer pageCount,
            String s3Key,
            PortfolioStatus status,
            String message,
            LocalDateTime createdAt,
            LocalDateTime uploadedAt,
            boolean replacement,
            boolean deleted,
            LocalDateTime deletedAt
    ) {
        return Portfolio.builder()
                .id(id)
                .userId(userId)
                .fileName(fileName)
                .fileSize(fileSize)
                .pageCount(pageCount)
                .s3Key(s3Key)
                .status(status)
                .message(message)
                .createdAt(createdAt)
                .uploadedAt(uploadedAt)
                .replacement(replacement)
                .deleted(deleted)
                .deletedAt(deletedAt)
                .build();
    }

    public boolean hasEnoughExtractedText(String extractedText) {
        return extractedText.trim().length() >= MIN_EXTRACTED_TEXT_LENGTH;
    }

    public void ready() {
        this.status = PortfolioStatus.READY;
        this.uploadedAt = LocalDateTime.now();
        this.message = "포트폴리오 처리가 완료되었습니다.";
    }

    public void failFile(String message) {
        this.status = PortfolioStatus.FAILED_FILE;
        this.message = message;
    }

    public void failSystem(String message) {
        this.status = PortfolioStatus.FAILED_SYSTEM;
        this.message = message;
    }

    public boolean failIfProcessingTimedOut() {
        if (status != PortfolioStatus.PROCESSING) {
            return false;
        }
        if (createdAt.plus(PROCESSING_TIMEOUT).isAfter(LocalDateTime.now())) {
            return false;
        }
        failSystem("처리 시간이 초과되었어요. 다시 시도해 주세요.");
        return true;
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
