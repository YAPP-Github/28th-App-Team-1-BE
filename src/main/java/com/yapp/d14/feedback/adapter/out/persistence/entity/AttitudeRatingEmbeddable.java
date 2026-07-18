package com.yapp.d14.feedback.adapter.out.persistence.entity;

import com.yapp.d14.feedback.domain.AttitudeAxis;
import com.yapp.d14.feedback.domain.AttitudeRating;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AttitudeRatingEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "axis", nullable = false)
    private AttitudeAxis axis;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    public static AttitudeRatingEmbeddable from(AttitudeRating rating) {
        return new AttitudeRatingEmbeddable(rating.axis(), rating.level(), rating.comment());
    }

    public AttitudeRating toDomain() {
        return new AttitudeRating(axis, level, comment);
    }
}
