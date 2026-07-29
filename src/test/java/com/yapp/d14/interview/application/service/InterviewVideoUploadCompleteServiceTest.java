package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.InterviewVideoCompositeUseCase;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        service.complete(USER_ID, SESSION_ID, null, null);

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
        service.complete(USER_ID, SESSION_ID, null, null);

        verify(interviewVideoCompositeUseCase).composite(USER_ID, SESSION_ID);
    }
}
