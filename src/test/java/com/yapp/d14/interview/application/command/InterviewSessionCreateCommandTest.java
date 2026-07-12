package com.yapp.d14.interview.application.command;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewSessionCreateCommandTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    @Test
    void 유효한_값이면_커맨드를_정상_생성한다() {
        InterviewSessionCreateCommand command =
                InterviewSessionCreateCommand.of(userId, portfolioId, "BACKEND", 3, null, null, null);

        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.portfolioId()).isEqualTo(portfolioId);
        assertThat(command.jobRole()).isEqualTo(JobType.BACKEND);
        assertThat(command.careerYears()).isEqualTo(3);
    }

    @Test
    void 정의되지_않은_jobRole이면_예외를_던진다() {
        assertThatThrownBy(() -> InterviewSessionCreateCommand.of(userId, portfolioId, "PM", 3, null, null, null))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_JOB_ROLE);
    }

    @Test
    void jobRole이_null이면_예외를_던진다() {
        assertThatThrownBy(() -> InterviewSessionCreateCommand.of(userId, portfolioId, null, 3, null, null, null))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_JOB_ROLE);
    }

    @Test
    void careerYears가_null이면_예외를_던진다() {
        assertThatThrownBy(() -> InterviewSessionCreateCommand.of(userId, portfolioId, "BACKEND", null, null, null, null))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_CAREER_YEARS);
    }

    @Test
    void careerYears가_음수면_예외를_던진다() {
        assertThatThrownBy(() -> InterviewSessionCreateCommand.of(userId, portfolioId, "BACKEND", -1, null, null, null))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INVALID_CAREER_YEARS);
    }

    @Test
    void careerYears가_0이면_정상_생성한다() {
        InterviewSessionCreateCommand command =
                InterviewSessionCreateCommand.of(userId, portfolioId, "BACKEND", 0, null, null, null);

        assertThat(command.careerYears()).isZero();
    }
}
