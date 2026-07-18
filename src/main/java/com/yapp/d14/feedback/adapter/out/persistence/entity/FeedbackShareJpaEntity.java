package com.yapp.d14.feedback.adapter.out.persistence.entity;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.FeedbackShare;
import com.yapp.d14.feedback.domain.FeedbackShareStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feedback_share")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackShareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FeedbackShareStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "feedback_share_axis", joinColumns = @JoinColumn(name = "feedback_share_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "axis", nullable = false)
    @BatchSize(size = 100)
    private List<AttitudeAxis> axes = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static FeedbackShareJpaEntity from(FeedbackShare feedbackShare) {
        FeedbackShareJpaEntity entity = new FeedbackShareJpaEntity();
        entity.id = feedbackShare.getId();
        entity.sessionId = feedbackShare.getSessionId();
        entity.token = feedbackShare.getToken();
        entity.status = feedbackShare.getStatus();
        entity.axes = new ArrayList<>(feedbackShare.getAxes());
        entity.createdAt = feedbackShare.getCreatedAt();
        return entity;
    }

    public FeedbackShare toDomain() {
        return FeedbackShare.of(id, sessionId, token, axes, status, createdAt);
    }
}
