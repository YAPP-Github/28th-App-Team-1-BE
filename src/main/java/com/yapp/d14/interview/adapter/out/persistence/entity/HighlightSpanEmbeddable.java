package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.TimeRange;
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
public class HighlightSpanEmbeddable {

    @Column(name = "start_sec")
    private Float startSec;

    @Column(name = "end_sec")
    private Float endSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone")
    private HighlightTone tone;

    public static HighlightSpanEmbeddable from(HighlightSpan highlightSpan) {
        return new HighlightSpanEmbeddable(
                highlightSpan.range().startSec(),
                highlightSpan.range().endSec(),
                highlightSpan.tone()
        );
    }

    public HighlightSpan toDomain() {
        return new HighlightSpan(new TimeRange(startSec, endSec), tone);
    }
}
