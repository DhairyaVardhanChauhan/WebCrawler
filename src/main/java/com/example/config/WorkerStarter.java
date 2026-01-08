package com.example.config;

import com.example.worker.CrawlWorker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class WorkerStarter implements ApplicationRunner {

    private final ExecutorService executorService;
    private final CrawlWorker crawlWorker;

    public WorkerStarter(ExecutorService executorService, CrawlWorker crawlWorker) {
        this.executorService = executorService;
        this.crawlWorker = crawlWorker;
    }

    @Override
    public void run(ApplicationArguments args) {

    }
}

