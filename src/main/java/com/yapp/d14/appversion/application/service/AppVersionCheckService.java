package com.yapp.d14.appversion.application.service;

import com.yapp.d14.appversion.application.command.AppVersionCheckCommand;
import com.yapp.d14.appversion.application.port.in.AppVersionCheckUseCase;
import com.yapp.d14.appversion.application.port.in.result.AppVersionCheckResult;
import com.yapp.d14.appversion.application.port.out.AppVersionPolicyRepository;
import com.yapp.d14.appversion.domain.AppVersion;
import com.yapp.d14.appversion.domain.AppVersionPolicy;
import com.yapp.d14.appversion.domain.UpdateType;
import com.yapp.d14.appversion.exception.AppVersionErrorCode;
import com.yapp.d14.appversion.exception.AppVersionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AppVersionCheckService implements AppVersionCheckUseCase {

    private final AppVersionPolicyRepository appVersionPolicyRepository;

    @Override
    public AppVersionCheckResult check(AppVersionCheckCommand command) {
        AppVersionPolicy policy = appVersionPolicyRepository.findByPlatform(command.platform())
                .orElseThrow(() -> new AppVersionException(AppVersionErrorCode.POLICY_NOT_FOUND));

        AppVersion current = AppVersion.parse(command.version());
        UpdateType updateType = policy.determineUpdateType(current);

        return new AppVersionCheckResult(
                updateType,
                policy.getLatestVersion().value(),
                policy.getMinSupportedVersion().value(),
                policy.getStoreUrl(),
                policy.resolveTitle(updateType),
                policy.resolveBody(updateType)
        );
    }
}
