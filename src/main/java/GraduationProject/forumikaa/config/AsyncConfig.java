package GraduationProject.forumikaa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

    @Value("${file.upload.thread-pool.core-size:3}")
    private int fileUploadCoreSize;

    @Value("${file.upload.thread-pool.max-size:10}")
    private int fileUploadMaxSize;

    @Value("${file.upload.thread-pool.queue-capacity:50}")
    private int fileUploadQueueCapacity;

    @Value("${file.batch.thread-pool.core-size:4}")
    private int fileBatchCoreSize;

    @Value("${file.batch.thread-pool.max-size:15}")
    private int fileBatchMaxSize;

    @Value("${file.batch.thread-pool.queue-capacity:75}")
    private int fileBatchQueueCapacity;

    @Bean("fileUploadExecutor")
    public Executor fileUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(fileUploadCoreSize);
        executor.setMaxPoolSize(fileUploadMaxSize);
        executor.setQueueCapacity(fileUploadQueueCapacity);
        executor.setThreadNamePrefix("file-upload-");
        executor.setKeepAliveSeconds(30);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("fileBatchExecutor")
    public Executor fileBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(fileBatchCoreSize);
        executor.setMaxPoolSize(fileBatchMaxSize);
        executor.setQueueCapacity(fileBatchQueueCapacity);
        executor.setThreadNamePrefix("file-batch-");
        executor.setKeepAliveSeconds(45);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
