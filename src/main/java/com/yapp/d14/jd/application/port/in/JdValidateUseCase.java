package com.yapp.d14.jd.application.port.in;

import com.yapp.d14.jd.application.command.JdValidateCommand;

public interface JdValidateUseCase {

    JdCrawlResult validate(JdValidateCommand command);
}
