package com.yapp.d14.portfolio.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Portfolio {

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
            LocalDateTime uploadedAt
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
    }

    public static Portfolio create(UUID id, UUID userId, String fileName, long fileSize, int pageCount, String s3Key) {
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
            LocalDateTime uploadedAt
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
                .build();
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
}
