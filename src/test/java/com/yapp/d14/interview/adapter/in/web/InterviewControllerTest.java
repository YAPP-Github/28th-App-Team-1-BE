package com.yapp.d14.interview.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.common.exception.GlobalExceptionHandler;
import com.yapp.d14.common.web.CurrentUserArgumentResolver;
import com.yapp.d14.interview.application.port.in.InterviewSessionCreateUseCase;
import com.yapp.d14.interview.application.port.in.InterviewSessionStatusUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionPollStatus;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;
import com.yapp.d14.ticket.exception.TicketErrorCode;
import com.yapp.d14.ticket.exception.TicketException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock
    private InterviewSessionCreateUseCase interviewSessionCreateUseCase;

    @Mock
    private InterviewSessionStatusUseCase interviewSessionStatusUseCase;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        InterviewController controller = new InterviewController(interviewSessionCreateUseCase, interviewSessionStatusUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String requestBody(Map<String, Object> overrides) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("portfolioId", portfolioId.toString());
        body.put("jobRole", "BACKEND");
        body.put("careerYears", 3);
        overrides.forEach((key, value) -> {
            if (value == null) {
                body.remove(key);
            } else {
                body.put(key, value);
            }
        });
        return objectMapper.writeValueAsString(body);
    }

    private String validRequestBody() throws Exception {
        return requestBody(Map.of());
    }

    @Test
    void 정상_요청이면_202와_함께_생성_결과를_반환한다() throws Exception {
        given(interviewSessionCreateUseCase.create(any()))
                .willReturn(new InterviewSessionCreateResult(1L, InterviewSessionStatus.PREPARING));

        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType("application/json")
                        .content(validRequestBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.sessionId").value(1))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.statusUrl").value("/api/v1/interview/sessions/1/status"));
    }

    @Test
    void portfolioId가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType("application/json")
                        .content(requestBody(new LinkedHashMap<>() {{ put("portfolioId", null); }})))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jobRole이_blank면_400() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType("application/json")
                        .content(requestBody(Map.of("jobRole", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void careerYears가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType("application/json")
                        .content(requestBody(new LinkedHashMap<>() {{ put("careerYears", null); }})))
                .andExpect(status().isBadRequest());
    }

    @Test
    void InterviewException이_발생하면_해당_에러코드로_응답한다() throws Exception {
        given(interviewSessionCreateUseCase.create(any()))
                .willThrow(new InterviewException(InterviewErrorCode.JD_NOT_VALIDATED));

        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType("application/json")
                        .content(validRequestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JD_NOT_VALIDATED"));
    }

    @Test
    void TicketException이_발생하면_403으로_응답한다() throws Exception {
        given(interviewSessionCreateUseCase.create(any()))
                .willThrow(new TicketException(TicketErrorCode.NO_REMAINING_TICKET));

        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType("application/json")
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NO_REMAINING_TICKET"));
    }

    @Test
    void PortfolioException_NOT_FOUND이_발생하면_404로_응답한다() throws Exception {
        given(interviewSessionCreateUseCase.create(any()))
                .willThrow(new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        mockMvc.perform(post("/api/v1/interview/sessions")
                        .contentType("application/json")
                        .content(validRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
    }

    @Test
    void 상태조회_PROCESSING이면_200과_함께_상태만_반환한다() throws Exception {
        given(interviewSessionStatusUseCase.getStatus(userId, 1L))
                .willReturn(new InterviewSessionStatusResult(InterviewSessionPollStatus.PROCESSING, null, null));

        mockMvc.perform(get("/api/v1/interview/sessions/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.summaryQuestion").doesNotExist());
    }

    @Test
    void 상태조회_READY이면_요약질문을_포함해_반환한다() throws Exception {
        LocalDateTime startedAt = LocalDateTime.now();
        InterviewSessionStatusResult.SummaryQuestion summaryQuestion =
                new InterviewSessionStatusResult.SummaryQuestion(10L, null, 0, 0);
        given(interviewSessionStatusUseCase.getStatus(userId, 1L))
                .willReturn(new InterviewSessionStatusResult(InterviewSessionPollStatus.READY, startedAt, summaryQuestion));

        mockMvc.perform(get("/api/v1/interview/sessions/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.summaryQuestion.questionId").value(10))
                .andExpect(jsonPath("$.data.summaryQuestion.turn.turnLevel").value(0))
                .andExpect(jsonPath("$.data.summaryQuestion.turn.depthLevel").value(0));
    }

    @Test
    void 상태조회_FAILED이면_200과_함께_상태만_반환한다() throws Exception {
        given(interviewSessionStatusUseCase.getStatus(userId, 1L))
                .willReturn(new InterviewSessionStatusResult(InterviewSessionPollStatus.FAILED, null, null));

        mockMvc.perform(get("/api/v1/interview/sessions/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    void 상태조회_세션이_없으면_404() throws Exception {
        given(interviewSessionStatusUseCase.getStatus(userId, 1L))
                .willThrow(new InterviewException(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/interview/sessions/1/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVIEW_SESSION_NOT_FOUND"));
    }
}
