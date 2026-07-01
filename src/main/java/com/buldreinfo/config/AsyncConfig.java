package com.buldreinfo.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	public static final String IMAGE_EXECUTOR_BEAN_NAME = "imageProcessingExecutor";
	public static final String IMAGE_THREAD_PREFIX = "ImageProcessor-";
	public static final String VIDEO_EXECUTOR_BEAN_NAME = "videoProcessingExecutor";
	public static final String VIDEO_THREAD_PREFIX = "VideoProcessor-";
	public static final String TRACKING_EXECUTOR_BEAN_NAME = "hitTrackingExecutor";
	public static final String TRACKING_THREAD_PREFIX = "HitTracker-";

	@Bean(name = IMAGE_EXECUTOR_BEAN_NAME)
	public TaskExecutor imageProcessingExecutor() {
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

	@Bean(name = TRACKING_EXECUTOR_BEAN_NAME)
	public TaskExecutor hitTrackingExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix(TRACKING_THREAD_PREFIX);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}