package com.yapp.d14.common.response;

import com.yapp.d14.common.web.TraceIdFilter;
import org.slf4j.MDC;

/**
 * 에러 응답 공통 포맷. {@code traceId}는 현재 요청의 MDC 값({@link TraceIdFilter})을 그대로 실어,
 * 사용자/프론트가 에러를 제보하면 이 값으로 CloudWatch에서 해당 요청의 로그를 바로 찾을 수 있게 한다.
 */
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        String traceId
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, code, message, MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }
}
