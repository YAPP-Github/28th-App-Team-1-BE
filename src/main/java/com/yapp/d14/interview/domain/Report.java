package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Report {

    private final Long id;
    private final Long sessionId;
    private final Double compositeScore;
    private final InternalGrade internalGrade;
    private final String headline;
    private final HeadlineBranch headlineBranch;
    private final ReportStatus status;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Report(
            Long id,
            Long sessionId,
            Double compositeScore,
            InternalGrade internalGrade,
            String headline,
            HeadlineBranch headlineBranch,
            ReportStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.compositeScore = compositeScore;
        this.internalGrade = internalGrade;
        this.headline = headline;
        this.headlineBranch = headlineBranch;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Report create(
            Long sessionId,
            Double compositeScore,
            InternalGrade internalGrade,
            String headline,
            HeadlineBranch headlineBranch,
            ReportStatus status
    ) {
        return Report.builder()
                .sessionId(sessionId)
                .compositeScore(compositeScore)
                .internalGrade(internalGrade)
                .headline(headline)
                .headlineBranch(headlineBranch)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 목록/상세 화면에 실제로 노출할 status. FAILED는 영상과 무관하게 그대로 노출한다.
     * 그 외에는 영상이 있고 아직 합성 전이며 대기 timeout(createdAt 기준) 전이면 GENERATING으로 가린다 —
     * 채점만 끝난 상태를 사용자에게 완료로 보여주지 않기 위함(#155). timeout을 넘기면 영상 없이 원래 status를 노출한다.
     */
    public ReportStatus effectiveStatus(InterviewVideo video) {
        if (status == ReportStatus.FAILED) {
            return ReportStatus.FAILED;
        }
        if (video != null && !video.isComposited() && !video.isCompositeOverdue(createdAt)) {
            return ReportStatus.GENERATING;
        }
        return status;
    }

    public static Report of(
            Long id,
            Long sessionId,
            Double compositeScore,
            InternalGrade internalGrade,
            String headline,
            HeadlineBranch headlineBranch,
            ReportStatus status,
            LocalDateTime createdAt
    ) {
        return Report.builder()
                .id(id)
                .sessionId(sessionId)
                .compositeScore(compositeScore)
                .internalGrade(internalGrade)
                .headline(headline)
                .headlineBranch(headlineBranch)
                .status(status)
                .createdAt(createdAt)
                .build();
    }
}
