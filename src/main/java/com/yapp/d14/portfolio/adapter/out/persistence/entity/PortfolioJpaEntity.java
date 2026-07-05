package com.yapp.d14.portfolio.adapter.out.persistence.entity;

import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long fileSize;

    private Integer pageCount;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortfolioStatus status;

    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime uploadedAt;

    public static PortfolioJpaEntity from(Portfolio portfolio) {
        PortfolioJpaEntity entity = new PortfolioJpaEntity();
        entity.id = portfolio.getId();
        entity.userId = portfolio.getUserId();
        entity.fileName = portfolio.getFileName();
        entity.fileSize = portfolio.getFileSize();
        entity.pageCount = portfolio.getPageCount();
        entity.s3Key = portfolio.getS3Key();
        entity.status = portfolio.getStatus();
        entity.message = portfolio.getMessage();
        entity.createdAt = portfolio.getCreatedAt();
        entity.uploadedAt = portfolio.getUploadedAt();
        return entity;
    }

    public Portfolio toDomain() {
        return Portfolio.of(
                id,
                userId,
                fileName,
                fileSize,
                pageCount,
                s3Key,
                status,
                message,
                createdAt,
                uploadedAt
        );
    }
}
