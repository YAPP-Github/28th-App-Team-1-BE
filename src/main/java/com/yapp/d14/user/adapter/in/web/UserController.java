package com.yapp.d14.user.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.user.adapter.in.web.request.UserNameRegisterHttpRequest;
import com.yapp.d14.user.adapter.in.web.request.UserProfileUpdateHttpRequest;
import com.yapp.d14.user.adapter.in.web.response.UserNameCheckHttpResponse;
import com.yapp.d14.user.adapter.in.web.response.UserProfileHttpResponse;
import com.yapp.d14.user.application.port.in.UserNameDuplicateCheckUseCase;
import com.yapp.d14.user.application.port.in.UserNameRegisterUseCase;
import com.yapp.d14.user.application.port.in.UserProfileQueryUseCase;
import com.yapp.d14.user.application.port.in.UserProfileUpdateUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
class UserController implements UserControllerDocs {

    private final UserNameRegisterUseCase userNameRegisterUseCase;
    private final UserNameDuplicateCheckUseCase userNameDuplicateCheckUseCase;
    private final UserProfileQueryUseCase userProfileQueryUseCase;
    private final UserProfileUpdateUseCase userProfileUpdateUseCase;

    @Override
    @PatchMapping("/me/name")
    public ResponseEntity<ApiResponse<Void>> registerName(
            @CurrentUser UUID userId,
            @Valid @RequestBody UserNameRegisterHttpRequest request
    ) {
        userNameRegisterUseCase.register(userId, request.name());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping("/name/check")
    public ResponseEntity<ApiResponse<UserNameCheckHttpResponse>> checkName(
            @RequestParam @NotBlank(message = "이름을 입력해주세요.") String name
    ) {
        boolean available = userNameDuplicateCheckUseCase.isAvailable(name);
        return ResponseEntity.ok(ApiResponse.ok(new UserNameCheckHttpResponse(available)));
    }

    @Override
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileHttpResponse>> getProfile(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(UserProfileHttpResponse.from(userProfileQueryUseCase.getProfile(userId))));
    }

    @Override
    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @CurrentUser UUID userId,
            @Valid @RequestBody UserProfileUpdateHttpRequest request
    ) {
        userProfileUpdateUseCase.update(request.toCommand(userId));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
