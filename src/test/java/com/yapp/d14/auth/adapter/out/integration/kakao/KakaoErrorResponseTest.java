package com.yapp.d14.auth.adapter.out.integration.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 이미_연결이_끊긴_유저_에러_code_minus101이면_NotRegisteredUser로_판별한다() throws Exception {
        KakaoErrorResponse response = objectMapper.readValue(
                "{\"msg\":\"NotRegisteredUserException\",\"code\":-101}", KakaoErrorResponse.class);

        assertThat(response.isNotRegisteredUser()).isTrue();
    }

    @Test
    void code가_minus101이_아니면_NotRegisteredUser가_아니다() throws Exception {
        KakaoErrorResponse response = objectMapper.readValue(
                "{\"msg\":\"InternalError\",\"code\":-1}", KakaoErrorResponse.class);

        assertThat(response.isNotRegisteredUser()).isFalse();
    }

    @Test
    void code가_없으면_NotRegisteredUser가_아니다() throws Exception {
        KakaoErrorResponse response = objectMapper.readValue("{\"msg\":\"unknown\"}", KakaoErrorResponse.class);

        assertThat(response.isNotRegisteredUser()).isFalse();
    }
}
