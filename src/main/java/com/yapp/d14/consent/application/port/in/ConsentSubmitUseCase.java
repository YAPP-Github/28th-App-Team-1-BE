package com.yapp.d14.consent.application.port.in;

import com.yapp.d14.consent.application.command.ConsentSubmitCommand;

public interface ConsentSubmitUseCase {

    void submit(ConsentSubmitCommand command);
}
