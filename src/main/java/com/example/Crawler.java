package com.example;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.hash.Hashing;

public class Crawler {

    static JedisPool jedisPool = new JedisPool(System.getenv().getOrDefault("REDIS_HOST", "localhost"), 6379);


    public static Jedis getJedis() {
        return jedisPool.getResource();
    }

    public static Document retrieveHTML(String url) throws IOException {

        Document document = Jsoup.connect(url).get();
//        System.out.println(document.html());
        return document;

    }

    private static String normalizeUrl(String url) throws URISyntaxException {
        URI uri = new URI(url).normalize();
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new URISyntaxException(url, "Missing scheme");
        }
        scheme = scheme.toLowerCase();
        String host = uri.getHost();
        if (host == null) {
            throw new URISyntaxException(url, "Missing host");
        }
        host = host.toLowerCase();

        int port = uri.getPort();
        boolean isDefaultPort =
                (port == -1) ||
                        (scheme.equals("http") && port == 80) ||
                        (scheme.equals("https") && port == 443);

        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        String query = uri.getRawQuery();
        return new URI(
                scheme,
                null,
                host,
                isDefaultPort ? -1 : port,
                path,
                query,
                null
        ).toString();
    }

    private static boolean isEncodingBomb(String url) {
        int count = 0;
        for (int i = 0; i < url.length() - 2; i++) {
            if (url.charAt(i) == '%' &&
                    Character.digit(url.charAt(i + 1), 16) != -1 &&
                    Character.digit(url.charAt(i + 2), 16) != -1) {
                count++;
            }
            if (count > 20) return true;
        }
        return false;
    }

    public static void extractProductData(Document document) {
        Elements products = document.select("li.product");
        for (Element product : products) {
            String productName = product.select(".product-name").text();
            String price = product.select(".product-price").text();
            String imgUrl = product.select(".product-image").attr("src");
            publishProduct(productName, price, imgUrl);
        }
    }

    private static void publishProduct(String name, String price, String img) {
        try (Jedis jedis = getJedis()) {
            jedis.rpush("products", String.join("|", name, price, img));
        }
    }

    private static String getDomain(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        if (host.startsWith("www.")) {
            return host.substring(4);
        }
        return host;
    }

    private static boolean acquireDomainLock(String domain, int delay) {
        String key = "domain:lock:" + domain;

        try (Jedis jedis = getJedis()) {
            SetParams params = new SetParams()
                    .nx()
                    .px(delay);

            String result = jedis.set(key, "1", params);
            return "OK".equals(result);
        }
    }

    private static boolean isValidData(String url) throws URISyntaxException {
        if (url == null || url.isBlank()) return false;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        int hashIndex = url.indexOf('#');
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }

        String lower = url.toLowerCase();
        if (lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".gif") ||
                lower.endsWith(".svg") ||
                lower.endsWith(".pdf") ||
                lower.endsWith(".zip") ||
                lower.endsWith(".rar") ||
                lower.endsWith(".exe") ||
                lower.endsWith(".mp4") ||
                lower.endsWith(".mp3") ||
                lower.endsWith(".avi") ||
                lower.endsWith(".mov")) {
            return false;
        }

        if (isEncodingBomb(url)) return false;
        if (lower.contains("facebook.com") ||
                lower.contains("instagram.com") ||
                lower.contains("linkedin.com") ||
                lower.contains("twitter.com") ||
                lower.contains("x.com") ||
                lower.contains("youtube.com") ||
                lower.contains("play.google.com") ||
                lower.contains("apps.apple.com")) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String query = uri.getRawQuery();
            if (query != null && query.length() > 300) {
                return false;
            }
            if (uri.getHost() == null) return false;
        } catch (URISyntaxException e) {
            return false;
        }

        return true;
    }

    private static boolean isDuplicateContent(String html) {
        String contentHash =
                Hashing.sha256()
                        .hashString(html, StandardCharsets.UTF_8)
                        .toString();

        try (Jedis jedis = getJedis()) {
            if (jedis.sadd("content:hashes", contentHash) == 0) {
                return true;
            }
        }
        return false;
    }

    public static void crawl() throws IOException, URISyntaxException {
        String rawUrl;
        try (Jedis jedis = getJedis()) {
            List<String> data = jedis.blpop(5, "queue", "queue:retry");
            if (data == null) return;
            rawUrl = data.get(1);
            jedis.lpush("queue:processing", rawUrl);
        }

        String url;
        try {
            url = normalizeUrl(rawUrl);
        } catch (Exception e) {
            try (Jedis jedis = getJedis()) {
                jedis.lrem("queue:processing", 1, rawUrl);
            }
            return;
        }
        System.out.println("Thread: " + Thread.currentThread().getName() + " fetched url -> " + url);
        if (!isValidData(url)) {
            try (Jedis jedis = getJedis()) {
                jedis.lrem("queue:processing", 1, rawUrl);
            }
            return;
        }

        String domain;
        try {
            domain = getDomain(url);
        } catch (Exception e) {
            try (Jedis jedis = getJedis()) {
                jedis.lrem("queue:processing", 1, rawUrl);
            }
            return;
        }
        String activeKey = "domain:active:" + domain;
        try (Jedis jedis = getJedis()) {
            long active = jedis.incr(activeKey);
            if (active > 3) {
                jedis.decr(activeKey);
                jedis.rpush("queue", url);
                jedis.lrem("queue:processing", 1, rawUrl);
                return;
            }
            RobotsRules robotsRules = RobotsHelper.getRobotsRules(domain);
            if (!RobotsHelper.isAllowedByRobots(url, robotsRules)) {
                System.out.println("Blocked by robots.txt: " + url);
                jedis.decr(activeKey);
                jedis.lrem("queue:processing", 1, rawUrl);
                return;
            }

            if (!acquireDomainLock(domain, robotsRules.crawlDelayMs)) {
                jedis.decr(activeKey);
                jedis.lrem("queue:processing", 1, rawUrl);
                jedis.rpush("queue", url);
                return;
            }

            if (jedis.sismember("visitedUrls", url)) {
                jedis.decr(activeKey);
                jedis.lrem("queue:processing", 1, rawUrl);
                return;
            }

            if (jedis.sadd("visitedUrls", url) == 0) {
                jedis.decr(activeKey);
                jedis.lrem("queue:processing", 1, rawUrl);
                return;
            }

            Document doc;
            try {
                doc = retrieveHTML(url);
            } catch (Exception e) {
                long retries = jedis.hincrBy("url:retries", url, 1);
                jedis.decr(activeKey);
                jedis.lrem("queue:processing", 1, rawUrl);
                jedis.rpush(retries <= 3 ? "queue:retry" : "queue:dead", url);
                return;
            }

            if (isDuplicateContent(doc.html())) {
                jedis.decr(activeKey);
                jedis.lrem("queue:processing", 1, rawUrl);
                jedis.hdel("url:retries", url);
                return;
            }

            extractProductData(doc);
            for (Element link : doc.select("a[href]")) {
                try {
                    String next = normalizeUrl(link.absUrl("href"));
                    if (isValidData(next) && jedis.sadd("seenUrls", next) == 1) {
                        jedis.rpush("queue", next);
                    }
                } catch (Exception ignored) {
                }
            }
            jedis.hdel("url:retries", url);
            jedis.decr(activeKey);
            jedis.lrem("queue:processing", 1, rawUrl);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        String seedUrl = "https://www.scrapingcourse.com/ecommerce";
        Document document = retrieveHTML(seedUrl);
        if (document != null) {
            System.out.println("Document fetched successfully!");
        }
        try (Jedis jedis = getJedis()) {
            jedis.lpush("queue", seedUrl);
        }

        System.out.println("Normalized " + normalizeUrl(seedUrl));
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            executorService.execute(() -> {
                while (true) {
                    try {
                        crawl();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
