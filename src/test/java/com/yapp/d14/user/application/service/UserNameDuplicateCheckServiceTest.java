package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserNameDuplicateCheckServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserNameDuplicateCheckService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 다른_사람이_이미_사용중인_이름이면_사용불가를_반환한다() {
        given(userRepository.existsByNameAndIdNot("홍길동", userId)).willReturn(true);

        assertThat(service.isAvailable(userId, "홍길동")).isFalse();
    }

    @Test
    void 아무도_안쓰거나_본인만_쓰는_이름이면_사용가능을_반환한다() {
        given(userRepository.existsByNameAndIdNot("홍길동", userId)).willReturn(false);

        assertThat(service.isAvailable(userId, "홍길동")).isTrue();
    }
}
