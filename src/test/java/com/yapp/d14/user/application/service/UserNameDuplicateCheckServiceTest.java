package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserNameDuplicateCheckServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserNameDuplicateCheckService service;

    @Test
    void 이미_사용중인_이름이면_사용불가를_반환한다() {
        given(userRepository.existsByName("홍길동")).willReturn(true);

        assertThat(service.isAvailable("홍길동")).isFalse();
    }

    @Test
    void 아무도_안쓰는_이름이면_사용가능을_반환한다() {
        given(userRepository.existsByName("홍길동")).willReturn(false);

        assertThat(service.isAvailable("홍길동")).isTrue();
    }
}
