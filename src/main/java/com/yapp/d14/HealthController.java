package com.yapp.d14;

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
