package com.yapp.d14.interview.domain;

// 원문(9961535) §8 종합 판정 표. 사용자에게 노출하지 않는 내부 채용 의사결정용 등급.
public enum InternalGrade {

    STRONG_HIRE(3.50),
    HIRE(3.00),
    LEAN(2.50),
    NO(2.00),
    STRONG_NO(1.00);

    private final double lowerBound;

    InternalGrade(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public static InternalGrade fromScore(double compositeScore) {
        for (InternalGrade grade : values()) {
            if (compositeScore >= grade.lowerBound) {
                return grade;
            }
        }
        return STRONG_NO;
    }

    public InternalGrade capAtNo() {
        return ordinal() < NO.ordinal() ? NO : this;
    }
}
