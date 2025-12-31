package com.example;

import java.io.IOException;
import java.net.URISyntaxException;

public class CrawlerRunnable implements Runnable{

    @Override
    public void run() {
        while(true){
            try {
                Crawler.crawl();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
