package com.example.client;

import com.example.crawlerHelper.CrawlTask;
import com.example.worker.CrawlWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@RestController
public class CrawlController {


    @Value("${server.port}")
    private String port;

    @Value("${spring.application.name}")
    private String serviceName;

    private final CrawlWorker worker;
    private final ExecutorService executorService;

    public CrawlController(CrawlWorker worker,ExecutorService executorService) {
        this.worker = worker;
        this.executorService = executorService;
    }

    @PostMapping("/crawl")
    public ResponseEntity<Void> crawl(@RequestBody CrawlTask task) {
        try {
            System.out.println(
                    "Handled by " + serviceName +
                            " on port " + port +
                            " | URL: " + task.getUrl()
            );
            executorService.submit(() -> worker.crawlOnce(task));
            return ResponseEntity.accepted().build();
        } catch (RejectedExecutionException e) {
            return ResponseEntity.status(429).build();
        }
    }

}


