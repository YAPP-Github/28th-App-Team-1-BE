package com.yapp.d14.appversion.application.port.in;

import com.yapp.d14.appversion.application.command.AppVersionCheckCommand;
import com.yapp.d14.appversion.application.port.in.result.AppVersionCheckResult;

public interface AppVersionCheckUseCase {

    AppVersionCheckResult check(AppVersionCheckCommand command);
}
