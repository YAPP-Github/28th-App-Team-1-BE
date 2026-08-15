package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
public class InterviewVideo {

    /**
     * 지인(공유 링크) 접근의 최대 보유기간. {@link VideoRetentionTrigger#GUEST_FIRST_SUBMITTED}(소유자 쪽
     * 단계형 expiresAt 연장 폭)와 현재 값이 같지만 별개 개념이므로 상수를 공유하지 않는다 — 한쪽만 바뀌어도
     * 다른 쪽에 영향을 주지 않아야 한다.
     */
    private static final Duration GUEST_MAX_RETENTION = Duration.ofDays(30);

    private final Long id;
    private final Long sessionId;
    private final LocalDateTime baseAt;
    private LocalDateTime expiresAt;
    private boolean deleted;
    // 프론트가 녹화본(raw.mp4) S3 업로드를 끝내고 complete를 호출하면 true. 합성 트리거 조건일 뿐, 재생 URL 발급 기준은 아니다.
    private boolean uploaded;
    // 녹화본에 답변·질문 음성을 합성한 final.mp4가 준비되면 true. 재생 URL(videoUrl)은 composited=true && 만료 전일 때만 발급한다(그 외 null).
    private boolean composited;
    // 면접관 마무리 멘트(종료 TTS)의 녹화 타임라인 재생 구간(초). 업로드 완료 시 프론트가 보고한다. 마무리 멘트가 없으면 null.
    private Float wrapUpStartSec;
    private Float wrapUpEndSec;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewVideo(
            Long id,
            Long sessionId,
            LocalDateTime baseAt,
            LocalDateTime expiresAt,
            boolean deleted,
            boolean uploaded,
            boolean composited,
            Float wrapUpStartSec,
            Float wrapUpEndSec
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.baseAt = baseAt;
        this.expiresAt = expiresAt;
        this.deleted = deleted;
        this.uploaded = uploaded;
        this.composited = composited;
        this.wrapUpStartSec = wrapUpStartSec;
        this.wrapUpEndSec = wrapUpEndSec;
    }

    /** 1차 레포트 생성 성공(Step1) 시점에 생성한다. baseAt은 이후 재계산하지 않는다. */
    public static InterviewVideo create(Long sessionId, LocalDateTime baseAt) {
        return create(sessionId, baseAt, null, null);
    }

    /** 업로드 완료 시점에 마무리 멘트 재생 구간과 함께 생성한다(upsert 후보). 마무리 멘트가 없으면 두 값 null. */
    public static InterviewVideo create(Long sessionId, LocalDateTime baseAt, Float wrapUpStartSec, Float wrapUpEndSec) {
        InterviewVideo video = InterviewVideo.builder()
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(baseAt)
                .deleted(false)
                .uploaded(false)
                .composited(false)
                .wrapUpStartSec(wrapUpStartSec)
                .wrapUpEndSec(wrapUpEndSec)
                .build();
        video.extend(VideoRetentionTrigger.REPORT_GENERATED);
        return video;
    }

    public static InterviewVideo of(
            Long id,
            Long sessionId,
            LocalDateTime baseAt,
            LocalDateTime expiresAt,
            boolean deleted,
            boolean uploaded,
            boolean composited,
            Float wrapUpStartSec,
            Float wrapUpEndSec
    ) {
        return InterviewVideo.builder()
                .id(id)
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(expiresAt)
                .deleted(deleted)
                .uploaded(uploaded)
                .composited(composited)
                .wrapUpStartSec(wrapUpStartSec)
                .wrapUpEndSec(wrapUpEndSec)
                .build();
    }

    /** 항상 더 긴 쪽을 적용한다. 여러 번 호출해도 안전(idempotent)하다. */
    public void extend(VideoRetentionTrigger trigger) {
        LocalDateTime candidate = baseAt.plus(trigger.getExtension());
        if (candidate.isAfter(expiresAt)) {
            expiresAt = candidate;
        }
    }

    public boolean isExpired() {
        return deleted || LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 합성 대기가 timeout을 넘겼는지 판단한다. 이미 합성됐으면 항상 false.
     * baseAt은 리포트 저장(Step1) 시점에 찍히므로, 녹화 업로드 자체가 영영 오지 않는 세션(조기 이탈 등)도
     * 이 기준으로 무한 대기하지 않고 fallback할 수 있다.
     */
    public boolean isCompositeOverdue(Duration timeout) {
        return !composited && LocalDateTime.now().isAfter(baseAt.plus(timeout));
    }

    /** 지인(공유 링크) 접근 전용 판정. 소유자 쪽 단계형 expiresAt과 무관하게 baseAt+30일(영상 최대보유기간) 하드캡으로만 판정한다. */
    public boolean isExpiredForGuest() {
        return deleted || LocalDateTime.now().isAfter(getGuestExpiresAt());
    }

    /** 지인 접근 하드캡 시각(baseAt+30일, 영상 최대보유기간). 소유자 쪽 단계형 expiresAt과는 다른 값이다. */
    public LocalDateTime getGuestExpiresAt() {
        return baseAt.plus(GUEST_MAX_RETENTION);
    }
}
