package com.yapp.d14.auth.adapter.in.web.response;

import java.util.UUID;

public record AuthCheckHttpResponse(String message, UUID userId) {
}
