package com.yapp.d14.feedback.adapter.out.persistence.entity;

import com.yapp.d14.feedback.domain.GuestFeedback;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
@Table(name = "guest_feedback")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestFeedbackJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "guest_feedback_rating", joinColumns = @JoinColumn(name = "guest_feedback_id"))
    @BatchSize(size = 100)
    private List<AttitudeRatingEmbeddable> ratings = new ArrayList<>();

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public static GuestFeedbackJpaEntity from(GuestFeedback guestFeedback) {
        GuestFeedbackJpaEntity entity = new GuestFeedbackJpaEntity();
        entity.id = guestFeedback.getId();
        entity.sessionId = guestFeedback.getSessionId();
        entity.nickname = guestFeedback.getNickname();
        entity.deviceId = guestFeedback.getDeviceId();
        entity.ratings = guestFeedback.getRatings().stream()
                .map(AttitudeRatingEmbeddable::from)
                .toList();
        entity.submittedAt = guestFeedback.getSubmittedAt();
        return entity;
    }

    public GuestFeedback toDomain() {
        return GuestFeedback.of(
                id,
                sessionId,
                nickname,
                deviceId,
                ratings.stream().map(AttitudeRatingEmbeddable::toDomain).toList(),
                submittedAt
        );
    }
}
