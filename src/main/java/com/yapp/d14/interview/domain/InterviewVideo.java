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
    // 프론트가 S3 업로드를 끝내고 complete를 호출하면 true. 재생 URL은 uploaded=true일 때만 발급한다.
    private boolean uploaded;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewVideo(
            Long id,
            Long sessionId,
            LocalDateTime baseAt,
            LocalDateTime expiresAt,
            boolean deleted,
            boolean uploaded
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.baseAt = baseAt;
        this.expiresAt = expiresAt;
        this.deleted = deleted;
        this.uploaded = uploaded;
    }

    /** 1차 레포트 생성 성공(Step1) 시점에 생성한다. baseAt은 이후 재계산하지 않는다. */
    public static InterviewVideo create(Long sessionId, LocalDateTime baseAt) {
        InterviewVideo video = InterviewVideo.builder()
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(baseAt)
                .deleted(false)
                .uploaded(false)
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
            boolean uploaded
    ) {
        return InterviewVideo.builder()
                .id(id)
                .sessionId(sessionId)
                .baseAt(baseAt)
                .expiresAt(expiresAt)
                .deleted(deleted)
                .uploaded(uploaded)
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
}
