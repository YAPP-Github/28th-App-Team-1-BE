package com.yapp.d14.auth.application.command;

import java.util.UUID;

public record LogoutCommand(UUID userId) {
}
