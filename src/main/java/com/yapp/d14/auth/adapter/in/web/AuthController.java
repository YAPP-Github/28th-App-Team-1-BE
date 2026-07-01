package com.yapp.d14.auth.adapter.in.web;

import com.yapp.d14.auth.adapter.in.web.request.SocialLoginHttpRequest;
import com.yapp.d14.auth.adapter.in.web.request.TokenReissueHttpRequest;
import com.yapp.d14.auth.adapter.in.web.response.AuthTokenHttpResponse;
import com.yapp.d14.auth.application.command.LogoutCommand;
import com.yapp.d14.auth.application.port.in.AuthToken;
import com.yapp.d14.auth.application.port.in.LogoutUseCase;
import com.yapp.d14.auth.application.port.in.SocialLoginUseCase;
import com.yapp.d14.auth.application.port.in.TokenReissueUseCase;
import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthController implements AuthControllerDocs {

    private final SocialLoginUseCase socialLoginUseCase;
    private final TokenReissueUseCase tokenReissueUseCase;
    private final LogoutUseCase logoutUseCase;

    @Override
    @PostMapping("/social/login")
    public ResponseEntity<ApiResponse<AuthTokenHttpResponse>> login(
            @Valid @RequestBody SocialLoginHttpRequest request
    ) {
        AuthToken authToken = socialLoginUseCase.login(request.toCommand());
        return ResponseEntity.ok(ApiResponse.ok(AuthTokenHttpResponse.from(authToken)));
    }

    @Override
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<AuthTokenHttpResponse>> reissue(
            @Valid @RequestBody TokenReissueHttpRequest request
    ) {
        AuthToken authToken = tokenReissueUseCase.reissue(request.toCommand());
        return ResponseEntity.ok(ApiResponse.ok(AuthTokenHttpResponse.from(authToken)));
    }

    @Override
    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUser UUID userId) {
        logoutUseCase.logout(new LogoutCommand(userId));
        return ResponseEntity.noContent().build();
    }
}
