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

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewVideo(
            Long id,
            Long sessionId,
            LocalDateTime baseAt,
            LocalDateTime expiresAt,
            boolean deleted
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.baseAt = baseAt;
        this.expiresAt = expiresAt;
        this.deleted = deleted;
    }

    /** 1차 레포트 생성 성공(Step1) 시점에 생성한다. baseAt은 이후 재계산하지 않는다. */
    public static InterviewVideo create(Long sessionId, LocalDateTime baseAt) {
        InterviewVideo video = InterviewVideo.builder()
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(baseAt)
                .deleted(false)
                .build();
        video.extend(VideoRetentionTrigger.REPORT_GENERATED);
        return video;
    }

    public static InterviewVideo of(
            Long id,
            Long sessionId,
            LocalDateTime baseAt,
            LocalDateTime expiresAt,
            boolean deleted
    ) {
        return InterviewVideo.builder()
                .id(id)
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(expiresAt)
                .deleted(deleted)
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
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
