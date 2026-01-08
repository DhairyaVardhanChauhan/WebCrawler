package com.example.worker;

import com.example.*;
import com.example.crawlerHelper.CrawlTask;
import com.example.crawlerHelper.Crawler;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.time.Duration;

import static com.example.crawlerHelper.Crawler.*;


@Service
public class CrawlWorker {

    private static JedisPool jedisPool;

    public CrawlWorker(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void crawlOnce(CrawlTask crawlTask){

        try(Jedis jedis = getJedis()) {
            String url = crawlTask.getUrl();
            String domain = crawlTask.getDomain();
            System.out.println("Domain: " + domain + "Url: " + url + " Thread: " + Thread.currentThread().getName());
            if (url == null) return;

            String activeKey = "domain:active:" + domain;
            Document doc;
            try {
                doc = Jsoup.connect(url).get();
            } catch (Exception e) {
                jedis.lrem("queue:processing", 1, url);
                jedis.rpush("queue", url);
                return;
            }

            for (Element link : doc.select("a[href]")) {
                try{
                    String next = link.absUrl("href");
                    if (jedis.sadd("seenUrls", next) == 1) {
                        jedis.rpush("queue", next);
                    }
                } catch (Exception ignored) {}
            }

            jedis.hdel("url:retries", url);
            jedis.decr(activeKey);
            jedis.lrem("queue:processing", 1, url);
        }
    }
}
