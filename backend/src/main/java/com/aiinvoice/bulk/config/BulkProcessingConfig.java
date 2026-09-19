package com.aiinvoice.bulk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BulkProcessingConfig {

    @Value("${bulk.worker-threads:5}")
    private int workerThreads;

    @Value("${bulk.queue-capacity:50}")
    private int queueCapacity;

    @Bean(name = "bulkWorkerPool")
    public ThreadPoolTaskExecutor bulkWorkerPool() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(workerThreads);
        ex.setMaxPoolSize(workerThreads * 2);
        ex.setQueueCapacity(queueCapacity);
        ex.setThreadNamePrefix("bulk-worker-");
        ex.initialize();
        return ex;
    }
}
