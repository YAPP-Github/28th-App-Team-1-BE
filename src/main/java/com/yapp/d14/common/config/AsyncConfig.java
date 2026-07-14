package com.yapp.d14.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "portfolioTaskExecutor")
    public Executor portfolioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        // 대기열에 원본 파일 byte[](최대 20MB)가 그대로 캡처되어 쌓이므로 여유 있게 잡지 않는다.
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("portfolio-async-");
        // 큐가 가득 차면 즉시 RejectedExecutionException을 던져 호출부(PortfolioRegisterService)에서 처리한다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "interviewPreloadTaskExecutor")
    public Executor interviewPreloadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("interview-preload-async-");
        // 큐가 가득 차면 즉시 RejectedExecutionException을 던져 호출부(InterviewSessionCreateService)에서 처리한다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
