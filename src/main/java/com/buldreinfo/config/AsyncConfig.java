package com.buldreinfo.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	public static final String IMAGE_EXECUTOR_BEAN_NAME = "imageProcessingExecutor";
	public static final String IMAGE_THREAD_PREFIX = "ImageProcessor-";
	public static final String VIDEO_EXECUTOR_BEAN_NAME = "videoProcessingExecutor";
	public static final String VIDEO_THREAD_PREFIX = "VideoProcessor-";

	@Bean(name = IMAGE_EXECUTOR_BEAN_NAME)
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix(IMAGE_THREAD_PREFIX);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

	@Bean(name = VIDEO_EXECUTOR_BEAN_NAME)
	public TaskExecutor videoProcessingExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix(VIDEO_THREAD_PREFIX);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}
