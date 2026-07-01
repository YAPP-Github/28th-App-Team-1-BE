package com.yapp.d14.auth.adapter.in.web;

import com.yapp.d14.common.web.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
class AuthCheckController {

    record AuthCheckResponse(String message, UUID userId) {}

    @GetMapping("/check")
    public ResponseEntity<AuthCheckResponse> check(@CurrentUser UUID userId) {
        return ResponseEntity.ok(new AuthCheckResponse("인증 성공", userId));
    }
}
