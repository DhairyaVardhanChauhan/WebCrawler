package com.example;

import com.google.gson.Gson;
import org.jsoup.Jsoup;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.awt.print.Pageable;
import java.io.IOException;
import java.net.URI;

import static com.example.Crawler.getJedis;

public class RobotsHelper{

    public static String fetchRobotsTxt(String domain){
        String url = "https://" + domain + "/robots.txt";
        try{
            String details = Jsoup.connect(url)
                    .userAgent("SimpleCrawler/1.0")
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute()
                    .body();
//            System.out.println("Details: " + details);
            return details;
        } catch (IOException e) {
            return null;
        }
    }

    public static RobotsRules parseRobots(String content){
        RobotsRules rules = new RobotsRules();
        boolean applies = false;
        if(content == null){
            return rules;
        }
        for(String line: content.split("\n")){
            if(line.startsWith("user-agent")){
                applies = line.contains("*");
            }
            else if(applies && line.startsWith("disallow:")){
                String path = line.split(":", 2)[1].trim();
                if (!path.isEmpty()) rules.getDisallowed().add(path);
            }
            else if(applies && line.startsWith("allow:")){
                String path = line.split(":",2)[1].trim();
                if(!path.isEmpty()) rules.getAllowed().add(path);
            }else if (applies && line.startsWith("crawl-delay:")) {
                try {
                    int seconds = Integer.parseInt(line.split(":", 2)[1].trim());
                    rules.crawlDelayMs = seconds * 1000;
                } catch (Exception ignored) {
                }
            }
        }
        return rules;
    }

    public static RobotsRules getRobotsRules(String domain) {
        String key = "robots:" + domain;
        try(Jedis jedis = getJedis()){
            String cached = jedis.get(key);
            if (cached != null) {
                return new Gson().fromJson(cached, RobotsRules.class);
            }
        }
        String robotsContent = fetchRobotsTxt(domain);
        RobotsRules parseRobotsRule = parseRobots(robotsContent);

        try (Jedis jedis = getJedis()) {
            jedis.setex(
                    key,
                    24 * 3600,
                    new Gson().toJson(parseRobotsRule)
            );
        }
        return parseRobotsRule;
    }


    public static boolean isAllowedByRobots(String url, RobotsRules rules) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            for (String allow : rules.getAllowed()) {
                if (path.startsWith(allow)) return true;
            }
            for (String disallow : rules.getDisallowed()) {
                if (path.startsWith(disallow)) return false;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

}
