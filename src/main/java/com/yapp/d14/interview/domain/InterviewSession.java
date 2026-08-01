package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
public class InterviewSession {

    // preload는 포트폴리오 청크 검색·JD 키워드 추출·캐물지점 추출·TTS 합성까지 순차 수행되는 작업이라 여유 있게 잡는다.
    private static final Duration PRELOAD_TIMEOUT = Duration.ofSeconds(45);

    private final Long id;
    private final UUID userId;
    private final UUID portfolioId;
    private final String portfolioFilename;
    private final JobType snapshotJobType;
    private final Integer snapshotYearsOfExperience;
    private final String jdUrl;
    private final String jdText;
    private final String focusProject;
    private final LocalDateTime createdAt;
    private InterviewSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private InterviewEndType endType;
    private AbandonCause abandonCause;
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
            String portfolioFilename,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject,
            LocalDateTime createdAt,
            InterviewSessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            InterviewEndType endType,
            AbandonCause abandonCause,
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
        this.portfolioFilename = portfolioFilename;
        this.snapshotJobType = snapshotJobType;
        this.snapshotYearsOfExperience = snapshotYearsOfExperience;
        this.jdUrl = jdUrl;
        this.jdText = jdText;
        this.focusProject = focusProject;
        this.createdAt = createdAt;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endType = endType;
        this.abandonCause = abandonCause;
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
            String portfolioFilename,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject
    ) {
        return InterviewSession.builder()
                .userId(userId)
                .portfolioId(portfolioId)
                .portfolioFilename(portfolioFilename)
                .snapshotJobType(snapshotJobType)
                .snapshotYearsOfExperience(snapshotYearsOfExperience)
                .jdUrl(jdUrl)
                .jdText(jdText)
                .focusProject(focusProject)
                .createdAt(LocalDateTime.now())
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

    // Portfolio.failIfProcessingTimedOut()과 동일한 패턴 — PREPARING에서 멈춘 세션을 폴링 시점에 감지한다.
    // 상태를 직접 바꾸지 않는 순수 판별만 한다 — 실제 실패 처리(후보/질문 삭제·이용권 release)는
    // InterviewPreloadFailureHandler.markFailed()가 전담해, preload()가 뒤늦게 성공해 덮어쓰는 경쟁을 막는다.
    public boolean isPreloadTimedOut() {
        return status == InterviewSessionStatus.PREPARING
                && !createdAt.plus(PRELOAD_TIMEOUT).isAfter(LocalDateTime.now());
    }

    public void markCompleted(InterviewEndType endType) {
        this.status = InterviewSessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
        this.endType = endType;
    }

    // 5-2/6-2장: 세션 누적 STT 인식 실패율이 30%를 초과해 세션을 완전 리셋(무효화)할 때 호출
    public void markInvalid(InterviewEndType endType) {
        this.status = InterviewSessionStatus.INVALID;
        this.endedAt = LocalDateTime.now();
        this.endType = endType;
    }

    // 재접속 화면의 "중단하기"(API B) 또는 hold 만료 read-repair(API A/A', cleanupExpiredHolds)에서 호출.
    // endType은 건드리지 않는다 — "왜 끊겼는지"(abandonCause)와 "면접 흐름이 어떻게 종료됐는지"(endType)는 별개 개념.
    public void markAbandoned(AbandonCause cause) {
        this.status = InterviewSessionStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
        this.abandonCause = cause;
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
            String portfolioFilename,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject,
            LocalDateTime createdAt,
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
            Integer sttTotalSegmentCount,
            AbandonCause abandonCause
    ) {
        return InterviewSession.builder()
                .id(id)
                .userId(userId)
                .portfolioId(portfolioId)
                .portfolioFilename(portfolioFilename)
                .snapshotJobType(snapshotJobType)
                .snapshotYearsOfExperience(snapshotYearsOfExperience)
                .jdUrl(jdUrl)
                .jdText(jdText)
                .focusProject(focusProject)
                .createdAt(createdAt)
                .status(status)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .endType(endType)
                .abandonCause(abandonCause)
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
