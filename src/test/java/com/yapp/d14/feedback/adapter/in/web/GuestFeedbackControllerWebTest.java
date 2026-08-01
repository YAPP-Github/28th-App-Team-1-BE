package com.yapp.d14.feedback.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.common.security.TokenParser;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackEntryUseCase;
import com.yapp.d14.feedback.application.port.in.GuestFeedbackSubmitUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Device-Id 헤더 필수 검증을 @Validated + @NotBlank(Docs 인터페이스) 선언으로 처리한다.
// 실제 Spring MVC 컨텍스트에서 헤더 누락은 ConstraintViolationException(→ 400 CONSTRAINT_VIOLATION)으로,
// @Valid 바디 검증은 구현체에서 @Valid를 떼고 인터페이스에만 둬도 그대로 동작함을 함께 고정한다.
// 게스트 엔드포인트는 무인증이라 보안 필터는 끈다.
@WebMvcTest(controllers = GuestFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestFeedbackControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GuestFeedbackEntryUseCase guestFeedbackEntryUseCase;

    @MockitoBean
    private GuestFeedbackSubmitUseCase guestFeedbackSubmitUseCase;

    @MockitoBean
    private TokenParser tokenParser;

    private String body(List<Map<String, Object>> ratings) throws Exception {
        return objectMapper.writeValueAsString(Map.of("nickname", "재원", "ratings", ratings));
    }

    @Test
    void 진입_DeviceId_헤더가_없으면_400_CONSTRAINT_VIOLATION() throws Exception {
        mockMvc.perform(get("/api/v1/feedback/guest/tok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION"))
                .andExpect(jsonPath("$.message").value("기기 식별 값이 필요해요."));
    }

    @Test
    void 제출_DeviceId_헤더가_없으면_400_CONSTRAINT_VIOLATION() throws Exception {
        mockMvc.perform(post("/api/v1/feedback/guest/tok/submissions")
                        .contentType("application/json")
                        .content(body(List.of(Map.of("axis", "GAZE", "level", 2)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION"))
                .andExpect(jsonPath("$.message").value("기기 식별 값이 필요해요."));
    }

    // 구현체에서 @Valid를 떼고 인터페이스에만 남겨도 @RequestBody 바디 검증이 그대로 도는지 고정한다.
    // 빈 ratings → @NotEmpty 위반 → VALIDATION_ERROR (커맨드의 INCOMPLETE_RATINGS 아님).
    @Test
    void 제출_DeviceId는_있고_바디가_비면_400_VALIDATION_ERROR() throws Exception {
        mockMvc.perform(post("/api/v1/feedback/guest/tok/submissions")
                        .header("Device-Id", "device-1")
                        .contentType("application/json")
                        .content(body(List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
