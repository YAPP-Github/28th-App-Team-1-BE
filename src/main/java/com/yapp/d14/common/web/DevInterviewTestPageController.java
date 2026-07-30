package com.yapp.d14.common.web;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 면접 수동 테스트용 정적 하네스 페이지(#78). classpath:/static/이 아니라 classpath:/dev-static/에 둬
// Spring Boot 기본 정적 리소스 매핑으로는 어떤 프로파일에서도 노출되지 않게 하고, 이 컨트롤러가 빈으로
// 등록되는 dev 프로파일에서만 "/interview-test.html"로 접근 가능하게 한다(운영 배포 시 404).
@Profile("dev")
@RestController
class DevInterviewTestPageController {

    private static final Resource PAGE = new ClassPathResource("dev-static/interview-test.html");

    @GetMapping("/interview-test.html")
    ResponseEntity<Resource> interviewTestPage() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(PAGE);
    }
}
