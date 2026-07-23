package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionOwnershipCheckUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewVideoUploadUrlResult;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage;
import com.yapp.d14.interview.application.port.out.InterviewVideoStorage.PresignedUpload;
import com.yapp.d14.interview.domain.InterviewVideo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewVideoUploadServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long SESSION_ID = 100L;

    @Mock
    private InterviewSessionOwnershipCheckUseCase interviewSessionOwnershipCheckUseCase;
    @Mock
    private InterviewVideoStorage interviewVideoStorage;
    @Mock
    private InterviewVideoRepository interviewVideoRepository;

    @InjectMocks
    private InterviewVideoUploadService service;

    @Test
    void 업로드_URL_발급은_소유권을_확인하고_presigned_PUT을_반환한다() {
        given(interviewVideoStorage.presignUpload(USER_ID, SESSION_ID))
                .willReturn(new PresignedUpload("https://s3/put", "video/webm", 600L));

        InterviewVideoUploadUrlResult result = service.issue(USER_ID, SESSION_ID);

        verify(interviewSessionOwnershipCheckUseCase).requireOwned(USER_ID, SESSION_ID);
        assertThat(result.uploadUrl()).isEqualTo("https://s3/put");
        assertThat(result.contentType()).isEqualTo("video/webm");
        assertThat(result.expiresInSeconds()).isEqualTo(600L);
    }

    @Test
    void 완료_확정은_소유권을_확인하고_uploaded_upsert를_호출한다() {
        service.complete(USER_ID, SESSION_ID);

        verify(interviewSessionOwnershipCheckUseCase).requireOwned(USER_ID, SESSION_ID);
        ArgumentCaptor<InterviewVideo> captor = ArgumentCaptor.forClass(InterviewVideo.class);
        verify(interviewVideoRepository).upsertUploaded(captor.capture());
        InterviewVideo candidate = captor.getValue();
        // 레코드가 없을 때 INSERT될 보관 타이머 값. expires_at은 미래(만료 전)여야 한다.
        assertThat(candidate.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(candidate.isExpired()).isFalse();
    }
}
