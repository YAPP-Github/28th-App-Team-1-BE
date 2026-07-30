package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewVideoUploadCompleteCommand;
import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoCompositeUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.exception.InterviewException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewVideoUploadCompleteServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long SESSION_ID = 100L;

    @Mock
    private InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    @Mock
    private InterviewVideoRepository interviewVideoRepository;
    @Mock
    private InterviewVideoCompositeUseCase interviewVideoCompositeUseCase;

    @InjectMocks
    private InterviewVideoUploadCompleteService service;

    @Test
    void 완료_확정은_소유권을_확인하고_uploaded_upsert를_호출한다() {
        service.complete(new InterviewVideoUploadCompleteCommand(USER_ID, SESSION_ID, null, null));

        verify(interviewSessionOwnershipCheckUseCase).requireOwned(USER_ID, SESSION_ID);
        ArgumentCaptor<InterviewVideo> captor = ArgumentCaptor.forClass(InterviewVideo.class);
        verify(interviewVideoRepository).upsertUploaded(captor.capture());
        InterviewVideo candidate = captor.getValue();
        // 레코드가 없을 때 INSERT될 보관 타이머 값. expires_at은 미래(만료 전)여야 한다.
        assertThat(candidate.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(candidate.isExpired()).isFalse();
    }

    @Test
    void 완료_확정_후_합성을_트리거한다() {
        // 유닛 테스트엔 활성 트랜잭션이 없어 runAfterCommit이 즉시 실행된다.
        service.complete(new InterviewVideoUploadCompleteCommand(USER_ID, SESSION_ID, null, null));

        verify(interviewVideoCompositeUseCase).composite(USER_ID, SESSION_ID);
    }

    @Test
    void 마무리_멘트_구간이_둘다_있으면_정상_저장된다() {
        service.complete(new InterviewVideoUploadCompleteCommand(USER_ID, SESSION_ID, 10.0f, 12.5f));

        ArgumentCaptor<InterviewVideo> captor = ArgumentCaptor.forClass(InterviewVideo.class);
        verify(interviewVideoRepository).upsertUploaded(captor.capture());
        assertThat(captor.getValue().getWrapUpStartSec()).isEqualTo(10.0f);
        assertThat(captor.getValue().getWrapUpEndSec()).isEqualTo(12.5f);
    }

    @ParameterizedTest
    @CsvSource({
            ", 12.5",      // start만 없음
            "10.0, ",      // end만 없음
            "-1.0, 12.5",  // start가 음수
            "12.5, 10.0",  // start >= end
            "10.0, 10.0",  // start == end
    })
    void 마무리_멘트_구간이_한쪽만_있거나_역전되면_예외를_던진다(Float wrapUpStartSec, Float wrapUpEndSec) {
        assertThatThrownBy(() ->
                service.complete(new InterviewVideoUploadCompleteCommand(USER_ID, SESSION_ID, wrapUpStartSec, wrapUpEndSec))
        ).isInstanceOf(InterviewException.class);

        verify(interviewVideoRepository, never()).upsertUploaded(org.mockito.ArgumentMatchers.any());
    }
}
