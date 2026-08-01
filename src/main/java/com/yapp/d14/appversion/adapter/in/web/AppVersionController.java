package com.yapp.d14.appversion.adapter.in.web;

import com.yapp.d14.appversion.adapter.in.web.response.AppVersionCheckHttpResponse;
import com.yapp.d14.appversion.application.command.AppVersionCheckCommand;
import com.yapp.d14.appversion.application.port.in.AppVersionCheckUseCase;
import com.yapp.d14.appversion.application.port.in.result.AppVersionCheckResult;
import com.yapp.d14.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app-versions")
@RequiredArgsConstructor
class AppVersionController implements AppVersionControllerDocs {

    private final AppVersionCheckUseCase appVersionCheckUseCase;

    @Override
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<AppVersionCheckHttpResponse>> check(
            @RequestParam String platform,
            @RequestParam String version
    ) {
        AppVersionCheckResult result = appVersionCheckUseCase.check(AppVersionCheckCommand.of(platform, version));
        return ResponseEntity.ok(ApiResponse.ok(AppVersionCheckHttpResponse.from(result)));
    }
}
