package com.yapp.d14.consent.application.command;

import java.util.List;
import java.util.UUID;

public record ConsentSubmitCommand(UUID userId, List<ConsentItemSubmission> items) {
}
