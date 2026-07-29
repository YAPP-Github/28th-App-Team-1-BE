package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InterviewVideo {

    private final Long id;
    private final Long sessionId;
    private final LocalDateTime baseAt;
    private LocalDateTime expiresAt;
    private boolean deleted;
    // 프론트가 녹화본(raw.mp4) S3 업로드를 끝내고 complete를 호출하면 true. 합성 트리거 조건일 뿐, 재생 URL 발급 기준은 아니다.
    private boolean uploaded;
    // 녹화본에 답변·질문 음성을 합성한 final.mp4가 준비되면 true. 재생 URL(videoUrl)은 composited=true && 만료 전일 때만 발급한다(그 외 null).
    private boolean composited;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewVideo(
            Long id,
            Long sessionId,
            LocalDateTime baseAt,
            LocalDateTime expiresAt,
            boolean deleted,
            boolean uploaded,
            boolean composited
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.baseAt = baseAt;
        this.expiresAt = expiresAt;
        this.deleted = deleted;
        this.uploaded = uploaded;
        this.composited = composited;
    }

    /** 1차 레포트 생성 성공(Step1) 시점에 생성한다. baseAt은 이후 재계산하지 않는다. */
    public static InterviewVideo create(Long sessionId, LocalDateTime baseAt) {
        InterviewVideo video = InterviewVideo.builder()
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(baseAt)
                .deleted(false)
                .uploaded(false)
                .composited(false)
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
            boolean composited
    ) {
        return InterviewVideo.builder()
                .id(id)
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(expiresAt)
                .deleted(deleted)
                .uploaded(uploaded)
                .composited(composited)
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

    /** 지인(공유 링크) 접근 전용 판정. 소유자 쪽 단계형 expiresAt과 무관하게 baseAt+30일(영상 최대보유기간) 하드캡으로만 판정한다. */
    public boolean isExpiredForGuest() {
        return deleted || LocalDateTime.now().isAfter(baseAt.plus(VideoRetentionTrigger.GUEST_FIRST_SUBMITTED.getExtension()));
    }
}
