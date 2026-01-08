package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class WorkerConfig {

    @Bean
    public ExecutorService crawlExecutor() {
        return new ThreadPoolExecutor(
                8, 8,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(50),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

}
