package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.InterviewVideo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_video")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewVideoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Column(name = "base_at", nullable = false)
    private LocalDateTime baseAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "uploaded", nullable = false, columnDefinition = "boolean default false")
    private boolean uploaded;

    public static InterviewVideoJpaEntity from(InterviewVideo interviewVideo) {
        InterviewVideoJpaEntity entity = new InterviewVideoJpaEntity();
        entity.id = interviewVideo.getId();
        entity.sessionId = interviewVideo.getSessionId();
        entity.baseAt = interviewVideo.getBaseAt();
        entity.expiresAt = interviewVideo.getExpiresAt();
        entity.deleted = interviewVideo.isDeleted();
        entity.uploaded = interviewVideo.isUploaded();
        return entity;
    }

    public InterviewVideo toDomain() {
        return InterviewVideo.of(id, sessionId, baseAt, expiresAt, deleted, uploaded);
    }
}
