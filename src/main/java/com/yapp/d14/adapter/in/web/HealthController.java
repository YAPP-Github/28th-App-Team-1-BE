package com.yapp.d14.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

    @GetMapping("/health")
    ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }
}
