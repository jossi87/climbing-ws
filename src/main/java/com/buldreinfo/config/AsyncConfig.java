package com.buldreinfo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    public static final String VIDEO_EXECUTOR_BEAN_NAME = "videoProcessingExecutor";
    public static final String VIDEO_THREAD_PREFIX = "VideoProcessor-";

    @Bean(name = VIDEO_EXECUTOR_BEAN_NAME)
    public Executor videoProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix(VIDEO_THREAD_PREFIX);
        executor.initialize();
        return executor;
    }
}