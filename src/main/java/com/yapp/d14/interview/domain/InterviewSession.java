package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
public class InterviewSession {

    private final Long id;
    private final UUID userId;
    private final UUID portfolioId;
    private final JobType snapshotJobType;
    private final Integer snapshotYearsOfExperience;
    private final String jdUrl;
    private final String jdText;
    private final String focusProject;
    private InterviewSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private InterviewEndType endType;
    private Integer weightDepth;
    private Integer weightBoundary;
    private Integer weightConnection;
    private Integer weightTradeoff;
    private Integer weightConflict;
    private Integer weightResilience;

    // 5-2장: 세션 전체 누적 STT 세그먼트 카운트(no_speech_prob>0.6인 세그먼트/전체 세그먼트). 30% 초과 시 STT_RESET.
    private Integer sttFailedSegmentCount;
    private Integer sttTotalSegmentCount;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewSession(
            Long id,
            UUID userId,
            UUID portfolioId,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject,
            InterviewSessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            InterviewEndType endType,
            Integer weightDepth,
            Integer weightBoundary,
            Integer weightConnection,
            Integer weightTradeoff,
            Integer weightConflict,
            Integer weightResilience,
            Integer sttFailedSegmentCount,
            Integer sttTotalSegmentCount
    ) {
        this.id = id;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.snapshotJobType = snapshotJobType;
        this.snapshotYearsOfExperience = snapshotYearsOfExperience;
        this.jdUrl = jdUrl;
        this.jdText = jdText;
        this.focusProject = focusProject;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endType = endType;
        this.weightDepth = weightDepth;
        this.weightBoundary = weightBoundary;
        this.weightConnection = weightConnection;
        this.weightTradeoff = weightTradeoff;
        this.weightConflict = weightConflict;
        this.weightResilience = weightResilience;
        this.sttFailedSegmentCount = sttFailedSegmentCount;
        this.sttTotalSegmentCount = sttTotalSegmentCount;
    }

    public static InterviewSession create(
            UUID userId,
            UUID portfolioId,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject
    ) {
        return InterviewSession.builder()
                .userId(userId)
                .portfolioId(portfolioId)
                .snapshotJobType(snapshotJobType)
                .snapshotYearsOfExperience(snapshotYearsOfExperience)
                .jdUrl(jdUrl)
                .jdText(jdText)
                .focusProject(focusProject)
                .status(InterviewSessionStatus.PREPARING)
                .sttFailedSegmentCount(0)
                .sttTotalSegmentCount(0)
                .build();
    }

    public void markReady() {
        this.status = InterviewSessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void markPreloadFailed() {
        this.status = InterviewSessionStatus.PRELOAD_FAILED;
    }

    public void markCompleted(InterviewEndType endType) {
        this.status = InterviewSessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
        this.endType = endType;
    }

    // 5-2/6-2장: 세션 누적 STT 인식 실패율이 30%를 초과해 세션을 완전 리셋(무효화)할 때 호출
    public void markInvalid() {
        this.status = InterviewSessionStatus.INVALID;
        this.endedAt = LocalDateTime.now();
    }

    // turnLevel≥1 매 턴 STT 변환 직후, 이번 턴의 세그먼트 통계를 세션 누적치에 더한다(SKIP 턴은 호출하지 않음).
    public void recordSttSegments(int failedSegmentCount, int totalSegmentCount) {
        this.sttFailedSegmentCount = (sttFailedSegmentCount == null ? 0 : sttFailedSegmentCount) + failedSegmentCount;
        this.sttTotalSegmentCount = (sttTotalSegmentCount == null ? 0 : sttTotalSegmentCount) + totalSegmentCount;
    }

    // 누적 실패 세그먼트 비율이 30%를 초과했는지 판단 (분모가 0이면 아직 판단 대상 아님)
    public boolean isSttFailureRateExceeded() {
        if (sttTotalSegmentCount == null || sttTotalSegmentCount == 0) {
            return false;
        }
        return (double) sttFailedSegmentCount / sttTotalSegmentCount > 0.3;
    }

    // FirstCoreAxisSelector 등 axis 우선순위 판단 로직에 넘길 때 쓰는 조회용 헬퍼
    public Map<TestType, Integer> getWeights() {
        return Map.of(
                TestType.DEPTH, weightDepth,
                TestType.BOUNDARY, weightBoundary,
                TestType.CONNECTION, weightConnection,
                TestType.TRADEOFF, weightTradeoff,
                TestType.CONFLICT, weightConflict,
                TestType.RESILIENCE, weightResilience
        );
    }

    public void assignWeights(Map<TestType, Integer> weights) {
        this.weightDepth = weights.get(TestType.DEPTH);
        this.weightBoundary = weights.get(TestType.BOUNDARY);
        this.weightConnection = weights.get(TestType.CONNECTION);
        this.weightTradeoff = weights.get(TestType.TRADEOFF);
        this.weightConflict = weights.get(TestType.CONFLICT);
        this.weightResilience = weights.get(TestType.RESILIENCE);
    }

    public static InterviewSession of(
            Long id,
            UUID userId,
            UUID portfolioId,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject,
            InterviewSessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            InterviewEndType endType,
            Integer weightDepth,
            Integer weightBoundary,
            Integer weightConnection,
            Integer weightTradeoff,
            Integer weightConflict,
            Integer weightResilience,
            Integer sttFailedSegmentCount,
            Integer sttTotalSegmentCount
    ) {
        return InterviewSession.builder()
                .id(id)
                .userId(userId)
                .portfolioId(portfolioId)
                .snapshotJobType(snapshotJobType)
                .snapshotYearsOfExperience(snapshotYearsOfExperience)
                .jdUrl(jdUrl)
                .jdText(jdText)
                .focusProject(focusProject)
                .status(status)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .endType(endType)
                .weightDepth(weightDepth)
                .weightBoundary(weightBoundary)
                .weightConnection(weightConnection)
                .weightTradeoff(weightTradeoff)
                .weightConflict(weightConflict)
                .weightResilience(weightResilience)
                .sttFailedSegmentCount(sttFailedSegmentCount)
                .sttTotalSegmentCount(sttTotalSegmentCount)
                .build();
    }
}
