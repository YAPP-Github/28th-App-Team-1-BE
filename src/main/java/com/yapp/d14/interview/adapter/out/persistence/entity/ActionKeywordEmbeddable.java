package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.ActionKeyword;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ActionKeywordEmbeddable {

    @Column(name = "keyword")
    private String keyword;

    @Column(name = "problem_analysis", columnDefinition = "TEXT")
    private String problemAnalysis;

    @Column(name = "improvement_reason", columnDefinition = "TEXT")
    private String improvementReason;

    @Column(name = "application_method", columnDefinition = "TEXT")
    private String applicationMethod;

    @Column(name = "priority")
    private int priority;

    public static ActionKeywordEmbeddable from(ActionKeyword actionKeyword) {
        return new ActionKeywordEmbeddable(
                actionKeyword.keyword(),
                actionKeyword.problemAnalysis(),
                actionKeyword.improvementReason(),
                actionKeyword.applicationMethod(),
                actionKeyword.priority()
        );
    }

    public ActionKeyword toDomain() {
        return new ActionKeyword(keyword, problemAnalysis, improvementReason, applicationMethod, priority);
    }
}
