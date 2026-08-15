package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.command.AiUsageRecordCommand;

public interface AiUsageRecordUseCase {

    void record(AiUsageRecordCommand command);
}
