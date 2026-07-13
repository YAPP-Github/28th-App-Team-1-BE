package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.TimeRange;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TimeRangeEmbeddable {

    @Column(name = "start_sec")
    private Float startSec;

    @Column(name = "end_sec")
    private Float endSec;

    public static TimeRangeEmbeddable from(TimeRange timeRange) {
        return new TimeRangeEmbeddable(timeRange.startSec(), timeRange.endSec());
    }

    public TimeRange toDomain() {
        return new TimeRange(startSec, endSec);
    }
}
