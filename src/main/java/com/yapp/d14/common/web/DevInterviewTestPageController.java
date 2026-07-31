package com.yapp.d14.common.web;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 면접 수동 테스트용 정적 하네스 페이지들. classpath:/static/이 아니라 classpath:/dev-static/에 둬
// Spring Boot 기본 정적 리소스 매핑으로는 어떤 프로파일에서도 노출되지 않게 하고, 이 컨트롤러가 빈으로
// 등록되는 dev 프로파일에서만 접근 가능하게 한다(운영 배포 시 404).
@Profile("dev")
@RestController
class DevInterviewTestPageController {

    // #78 문장 발화 시각 확인용 하네스
    private static final Resource SCRIPT_TIMING_PAGE = new ClassPathResource("dev-static/interview-test.html");

    // 면접 진행 전반(녹음·MP3 인코딩 포함)을 다루는 범용 하네스
    private static final Resource HARNESS_PAGE = new ClassPathResource("dev-static/interview-harness/index.html");
    private static final Resource HARNESS_LAME_JS = new ClassPathResource("dev-static/interview-harness/lame.min.js");

    @GetMapping("/interview-test.html")
    ResponseEntity<Resource> interviewTestPage() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(SCRIPT_TIMING_PAGE);
    }

    @GetMapping({"/interview-harness", "/interview-harness/", "/interview-harness/index.html"})
    ResponseEntity<Resource> interviewHarnessPage() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(HARNESS_PAGE);
    }

    @GetMapping("/interview-harness/lame.min.js")
    ResponseEntity<Resource> interviewHarnessLameJs() {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/javascript")).body(HARNESS_LAME_JS);
    }
}
