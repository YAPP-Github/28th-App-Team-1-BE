package com.yapp.d14.jd.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.jd.adapter.in.web.request.JdValidateHttpRequest;
import com.yapp.d14.jd.adapter.in.web.response.JdValidateHttpResponse;
import com.yapp.d14.jd.application.port.in.JdCrawlResult;
import com.yapp.d14.jd.application.port.in.JdValidateUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jd")
@RequiredArgsConstructor
class JdController implements JdControllerDocs {

    private final JdValidateUseCase jdValidateUseCase;

    @Override
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<JdValidateHttpResponse>> validate(
            @CurrentUser UUID userId,
            @Valid @RequestBody JdValidateHttpRequest request
    ) {
        JdCrawlResult result = jdValidateUseCase.validate(request.toCommand(userId));
        return ResponseEntity.ok(ApiResponse.ok(JdValidateHttpResponse.from(result)));
    }
}
