package com.yapp.d14.user.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.user.adapter.in.web.request.TokenReissueHttpRequest;
import com.yapp.d14.user.adapter.in.web.request.UserSocialLoginHttpRequest;
import com.yapp.d14.user.adapter.in.web.response.UserSocialLoginHttpResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.user.application.command.LogoutCommand;
import com.yapp.d14.user.application.port.in.AuthToken;
import com.yapp.d14.user.application.port.in.LogoutUseCase;
import com.yapp.d14.user.application.port.in.TokenReissueUseCase;
import com.yapp.d14.user.application.port.in.UserSocialLoginUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthController implements AuthControllerDocs {

    private final UserSocialLoginUseCase userSocialLoginUseCase;
    private final TokenReissueUseCase tokenReissueUseCase;
    private final LogoutUseCase logoutUseCase;

    @Override
    @PostMapping("/social/login")
    public ResponseEntity<ApiResponse<UserSocialLoginHttpResponse>> login(
            @Valid @RequestBody UserSocialLoginHttpRequest request
    ) {
        AuthToken authToken = userSocialLoginUseCase.login(request.toCommand());
        return ResponseEntity.ok(ApiResponse.ok(UserSocialLoginHttpResponse.from(authToken)));
    }

    @Override
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<UserSocialLoginHttpResponse>> reissue(
            @Valid @RequestBody TokenReissueHttpRequest request
    ) {
        AuthToken authToken = tokenReissueUseCase.reissue(request.toCommand());
        return ResponseEntity.ok(ApiResponse.ok(UserSocialLoginHttpResponse.from(authToken)));
    }

    @Override
    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUser UUID userId) {
        logoutUseCase.logout(new LogoutCommand(userId));
        return ResponseEntity.noContent().build();
    }
}
